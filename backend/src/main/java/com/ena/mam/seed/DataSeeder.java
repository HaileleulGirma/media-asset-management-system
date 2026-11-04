package com.ena.mam.seed;

import com.ena.mam.approle.AppRole;
import com.ena.mam.approle.AppRoleRepository;
import com.ena.mam.appuser.AppUser;
import com.ena.mam.appuser.AppUserRepository;
import com.ena.mam.cameraman.Cameraman;
import com.ena.mam.cameraman.CameramanRepository;
import com.ena.mam.location.Location;
import com.ena.mam.location.LocationRepository;
import com.ena.mam.news.News;
import com.ena.mam.news.NewsRepository;
import com.ena.mam.reporter.Reporter;
import com.ena.mam.reporter.ReporterRepository;
import com.ena.mam.staffmember.StaffMember;
import com.ena.mam.staffmember.StaffMemberRepository;
import com.github.javafaker.Faker;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Supplier;

/**
 * Dev/test data for the V1 schema.
 *
 * Every section checks whether its own table already has rows and skips itself if so,
 * so restarting the app never duplicates data or trips the UNIQUE constraints.
 *
 * Tables that already had JPA entities (reporter, cameraman, location, staff_member, news,
 * roles/users) are seeded through the repositories. The newer tables (cassette_category,
 * photo, digitized_media, cassette_cut, production) are seeded with JdbcTemplate straight
 * against the schema, so this class does not depend on their entity classes.
 */
@Component
//@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final ZoneId ADDIS = ZoneId.of("Africa/Addis_Ababa");

    private static final int REPORTERS = 100;
    private static final int CAMERAMEN = 100;
    private static final int LOCATIONS = 50;
    private static final int STAFF_MEMBERS = 45;
    private static final int NEWS = 5000;      // scale to 1M later
    private static final int PHOTOS = 2000;
    private static final int CASSETTES = 500;  // each gets 1-6 cuts
    private static final int PRODUCTIONS = 500;
    private static final int BATCH_SIZE = 1000;

    private static final String[] CASSETTE_CATEGORIES = {
            "ዜና", "ዶክመንተሪ", "ቃለ መጠይቅ", "ጉባኤ", "ሙዚቃ", "ድራማ", "ስፖርት", "መዝገብ"
    };

    // Includes look-alike spellings on purpose (ጸ/ፀ, ሰ/ሠ, አ/ዐ, ሀ/ሐ/ኀ/ኃ) so the
    // amh_norm() trigram search has something to normalize while you test it.
    private static final String[] AMHARIC_WORDS = {
            "ጸሎት", "ፀሎት", "ሰላም", "ሠላም", "ኢትዮጵያ", "አዲስ", "ዐዲስ", "አበባ", "ዜና", "ስብሰባ",
            "ጠቅላይ", "ሚኒስትር", "ምክር", "ቤት", "ልማት", "ትምህርት", "ጤና", "ግብርና", "መንገድ",
            "ኃይል", "ሃይል", "ሐምሌ", "ንግድ", "ወጣቶች", "ሴቶች", "ጉባኤ", "ኮንፈረንስ",
            "ኢንቨስትመንት", "ሥራ", "ስራ", "ዓመት", "አመት"
    };

    private final ReporterRepository reporterRepository;
    private final CameramanRepository cameramanRepository;
    private final LocationRepository locationRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final NewsRepository newsRepository;
    private final AppRoleRepository appRoleRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    private final Random random = new Random(42);
    private final Faker faker = new Faker(random);

    public DataSeeder(
            ReporterRepository reporterRepository,
            CameramanRepository cameramanRepository,
            LocationRepository locationRepository,
            StaffMemberRepository staffMemberRepository,
            NewsRepository newsRepository,
            AppRoleRepository appRoleRepository,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbc
    ) {
        this.reporterRepository = reporterRepository;
        this.cameramanRepository = cameramanRepository;
        this.locationRepository = locationRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.newsRepository = newsRepository;
        this.appRoleRepository = appRoleRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        System.out.println("Starting seeding...");

        // Lookups first, then everything that references them.
        seedRolesAndUsers();
        seedReporters();
        seedCameramen();
        seedLocations();
        seedStaffMembers();
        seedCassetteCategories();

        seedNews();
        seedPhotos();
        seedDigitizedMedia();
        seedProductions();

        System.out.println("Data seeding completed.");
    }

    // =====================================================
    // Lookups (JPA)
    // =====================================================

    private void seedReporters() {
        if (reporterRepository.count() > 0) return;

        // reporter_name is UNIQUE, so faker collisions must be filtered out.
        List<Reporter> list = new ArrayList<>();
        for (String name : uniqueNames(REPORTERS, () -> faker.name().fullName())) {
            Reporter r = new Reporter();
            r.setReporterName(name);
            r.setActive(random.nextInt(10) < 8); // ~80% active
            list.add(r);
        }
        reporterRepository.saveAll(list);
        reporterRepository.flush();
        entityManager.clear();
    }

    private void seedCameramen() {
        if (cameramanRepository.count() > 0) return;

        List<Cameraman> list = new ArrayList<>();
        for (String name : uniqueNames(CAMERAMEN, () -> faker.name().fullName())) {
            Cameraman c = new Cameraman();
            c.setCameramanName(name);
            c.setActive(random.nextInt(10) < 8); // ~80% active
            list.add(c);
        }
        cameramanRepository.saveAll(list);
        cameramanRepository.flush();
        entityManager.clear();
    }

    private void seedLocations() {
        if (locationRepository.count() > 0) return;

        // location_name is UNIQUE and there are only ~250 capitals to draw from,
        // so 50 random picks will collide without this.
        List<Location> list = new ArrayList<>();
        for (String name : uniqueNames(LOCATIONS, () -> faker.country().capital())) {
            Location l = new Location();
            l.setLocationName(name);
            l.setAbroad(random.nextBoolean());
            list.add(l);
        }
        locationRepository.saveAll(list);
        locationRepository.flush();
        entityManager.clear();
    }

    private void seedStaffMembers() {
        if (staffMemberRepository.count() > 0) return;

        List<StaffMember> list = new ArrayList<>();
        for (String name : uniqueNames(STAFF_MEMBERS, () -> faker.name().fullName())) {
            StaffMember s = new StaffMember();
            s.setStaffMemberName(name);
            s.setActive(true);
            list.add(s);
        }
        staffMemberRepository.saveAll(list);
        staffMemberRepository.flush();
        entityManager.clear();
    }

    // =====================================================
    // News (JPA)
    // =====================================================

    private void seedNews() {
        if (newsRepository.count() > 0) return;

        List<Reporter> reporters = reporterRepository.findAll();
        List<Cameraman> cameramen = cameramanRepository.findAll();
        List<Location> locations = locationRepository.findAll();
        List<StaffMember> staffMembers = staffMemberRepository.findAll();

        List<News> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < NEWS; i++) {

            News news = new News();

            LocalDate newsDate = randomDate();

            news.setTitle(randomTitle());
            news.setPrivate(faker.bool().bool());
            news.setNewsDate(newsDate);
            news.setFilePath(datedPath("/mnt/media", newsDate));
            news.setNumberOfFiles(1 + random.nextInt(100));
            news.setTotalSize(BigDecimal.valueOf(round2(10 + random.nextDouble() * 5000)));

            news.setImporter(staffMembers.get(random.nextInt(staffMembers.size())));
            // ingested_by is nullable: ingestion happens after import, so ~15% are not ingested yet.
            news.setIngestor(random.nextInt(100) < 15
                    ? null
                    : staffMembers.get(random.nextInt(staffMembers.size())));

            news.setReporters(randomSubset(reporters, 10));
            news.setCameramen(randomSubset(cameramen, 10));
            news.setLocations(randomSubset(locations, 5));

            batch.add(news);

            if (batch.size() == BATCH_SIZE) {
                newsRepository.saveAll(batch);
                newsRepository.flush();
                entityManager.clear(); // keeps the persistence context from growing
                batch.clear();
                System.out.println("Inserted " + (i + 1) + " news records");
            }
        }

        if (!batch.isEmpty()) {
            newsRepository.saveAll(batch);
            newsRepository.flush();
            entityManager.clear();
        }
    }

    // =====================================================
    // Newer tables (JdbcTemplate, straight against the schema)
    // =====================================================

    private void seedCassetteCategories() {
        if (count("cassette_category") > 0) return;

        List<Object[]> rows = new ArrayList<>();
        for (String name : CASSETTE_CATEGORIES) {
            rows.add(new Object[]{name});
        }
        jdbc.batchUpdate("INSERT INTO cassette_category (category_name) VALUES (?)", rows);
    }

    private void seedPhotos() {
        if (count("photo") > 0) return;

        List<Long> staffIds = staffMemberIds();
        List<Long> cameramanIds = cameramanIds();
        List<Long> locationIds = locationIds();

        List<Object[]> cameramanLinks = new ArrayList<>();
        List<Object[]> locationLinks = new ArrayList<>();

        for (int i = 0; i < PHOTOS; i++) {
            LocalDate photoDate = randomDate();

            // photo_id is GENERATED ALWAYS, so let the DB assign it and read it back.
            Long photoId = jdbc.queryForObject(
                    "INSERT INTO photo (title, file_path, number_of_files, file_size_mb, photo_date, private, imported_by) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING photo_id",
                    Long.class,
                    randomTitle(),
                    datedPath("/mnt/photos", photoDate),
                    1 + random.nextInt(300),
                    round2(5 + random.nextDouble() * 800),
                    java.sql.Date.valueOf(photoDate),
                    faker.bool().bool(),
                    pick(staffIds)
            );

            for (Long cameramanId : randomSubset(cameramanIds, 3)) {
                cameramanLinks.add(new Object[]{photoId, cameramanId});
            }
            for (Long locationId : randomSubset(locationIds, 3)) {
                locationLinks.add(new Object[]{photoId, locationId});
            }
        }

        jdbc.batchUpdate("INSERT INTO photo_cameraman (photo_id, cameraman_id) VALUES (?, ?)", cameramanLinks);
        jdbc.batchUpdate("INSERT INTO photo_location (photo_id, location_id) VALUES (?, ?)", locationLinks);
        System.out.println("Inserted " + PHOTOS + " photo records");
    }

    // One digitized_media row = one physical cassette; its content titles live in cassette_cut.
    private void seedDigitizedMedia() {
        if (count("digitized_media") > 0) return;

        List<Long> staffIds = staffMemberIds();
        List<Long> categoryIds = ids("SELECT cassette_category_id FROM cassette_category");

        // (identifier_category_id, identifier_number) is UNIQUE: number cassettes
        // sequentially within each category.
        Map<Long, Integer> nextNumber = new HashMap<>();

        List<Object[]> cuts = new ArrayList<>();

        for (int i = 0; i < CASSETTES; i++) {
            LocalDate digitizedDate = randomDate();
            Long identifierCategoryId = pick(categoryIds);
            int identifierNumber = nextNumber.merge(identifierCategoryId, 1, Integer::sum);

            Long mediaId = jdbc.queryForObject(
                    "INSERT INTO digitized_media (digitized_date, identifier_category_id, identifier_number, "
                            + "file_path, private, imported_by) VALUES (?, ?, ?, ?, ?, ?) RETURNING digitized_media_id",
                    Long.class,
                    java.sql.Date.valueOf(digitizedDate),
                    identifierCategoryId,
                    identifierNumber,
                    "/mnt/cassettes/" + identifierCategoryId + "/" + identifierNumber,
                    faker.bool().bool(),
                    pick(staffIds)
            );

            // Each cut has its own category (may differ from the cassette's identifier category)
            // and an optional date.
            int cutCount = 1 + random.nextInt(6);
            for (int c = 0; c < cutCount; c++) {
                java.sql.Date cutDate = random.nextInt(10) < 8
                        ? java.sql.Date.valueOf(randomDate())
                        : null;
                cuts.add(new Object[]{mediaId, pick(categoryIds), randomTitle(), cutDate});
            }
        }

        // Explicit types so the nullable cut_date binds cleanly.
        jdbc.batchUpdate(
                "INSERT INTO cassette_cut (digitized_media_id, cassette_category_id, cut_title, cut_date) VALUES (?, ?, ?, ?)",
                cuts,
                new int[]{Types.BIGINT, Types.BIGINT, Types.VARCHAR, Types.DATE}
        );
        System.out.println("Inserted " + CASSETTES + " cassettes with " + cuts.size() + " cuts");
    }

    private void seedProductions() {
        if (count("production") > 0) return;

        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < PRODUCTIONS; i++) {
            LocalDate productionDate = randomDate();
            rows.add(new Object[]{
                    java.sql.Date.valueOf(productionDate),
                    randomTitle(),
                    datedPath("/mnt/production", productionDate),
                    round2(1 + random.nextDouble() * 200),
                    60 + random.nextInt(7200) // production_duration_sec, 1 min to 2 h
            });
        }

        jdbc.batchUpdate(
                "INSERT INTO production (production_date, title, file_path, file_size_gb, production_duration_sec) "
                        + "VALUES (?, ?, ?, ?, ?)",
                rows,
                new int[]{Types.DATE, Types.VARCHAR, Types.VARCHAR, Types.NUMERIC, Types.INTEGER}
        );
        System.out.println("Inserted " + PRODUCTIONS + " production records");
    }

    // =====================================================
    // Roles and users (idempotent)
    // =====================================================

    private void seedRolesAndUsers() {
        AppRole adminRole = getOrCreateRole("ADMIN");
        AppRole staffRole = getOrCreateRole("STAFF");
        AppRole viewerRole = getOrCreateRole("VIEWER");

        getOrCreateUser("admin", "admin123", "System Administrator", adminRole);
        getOrCreateUser("staff1", "staff123", "Staff User", staffRole);
        getOrCreateUser("viewer1", "viewer123", "Reporter Viewer", viewerRole);
    }

    private AppRole getOrCreateRole(String name) {
        return appRoleRepository.findByRoleName(name)
                .orElseGet(() -> {
                    AppRole role = new AppRole();
                    role.setRoleName(name);
                    return appRoleRepository.save(role);
                });
    }

    private void getOrCreateUser(String username, String rawPassword, String fullName, AppRole role) {
        if (appUserRepository.findByUsernameWithRoles(username).isPresent()) return;
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setRole(role);
        user.setLastChanged(LocalDate.now(ADDIS));
        appUserRepository.save(user);
    }

    // =====================================================
    // Helpers
    // =====================================================

    // ~15% of dates fall in the last 14 days so the staff 7-day edit window has
    // both editable and locked rows to test; the rest spread over ten years.
    private LocalDate randomDate() {
        int daysBack = random.nextInt(100) < 15 ? random.nextInt(14) : random.nextInt(3650);
        return LocalDate.now(ADDIS).minusDays(daysBack);
    }

    // ~70% Amharic (with look-alike letter variants), ~30% Latin filler.
    private String randomTitle() {
        if (random.nextInt(10) < 7) {
            int words = 2 + random.nextInt(4);
            StringJoiner joiner = new StringJoiner(" ");
            for (int i = 0; i < words; i++) {
                joiner.add(AMHARIC_WORDS[random.nextInt(AMHARIC_WORDS.length)]);
            }
            return joiner.toString();
        }
        return faker.lorem().sentence(3 + random.nextInt(12));
    }

    private String datedPath(String root, LocalDate date) {
        return String.format("%s/%d/%02d/%02d", root, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Set<String> uniqueNames(int count, Supplier<String> generator) {
        Set<String> names = new LinkedHashSet<>();
        int attempts = 0;
        while (names.size() < count) {
            if (++attempts > count * 100) {
                throw new IllegalStateException("Could not generate " + count + " unique names");
            }
            String name = generator.get();
            if (name != null && !name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    private <T> T pick(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    private <T> Set<T> randomSubset(List<T> list, int maxSize) {
        Set<T> result = new HashSet<>();
        int size = 1 + random.nextInt(Math.min(maxSize, list.size()));
        while (result.size() < size) {
            result.add(list.get(random.nextInt(list.size())));
        }
        return result;
    }

    // Ids come from the entities, so the seeder follows whatever column names the
    // entities map (the staff_member columns differ between the entity and V1).
    private List<Long> staffMemberIds() {
        List<Long> ids = new ArrayList<>();
        staffMemberRepository.findAll().forEach(s -> ids.add(s.getStaffMemberId()));
        return ids;
    }

    private List<Long> cameramanIds() {
        List<Long> ids = new ArrayList<>();
        cameramanRepository.findAll().forEach(c -> ids.add(c.getCameramanId()));
        return ids;
    }

    private List<Long> locationIds() {
        List<Long> ids = new ArrayList<>();
        locationRepository.findAll().forEach(l -> ids.add(l.getLocationId()));
        return ids;
    }

    private long count(String table) {
        Long c = jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
        return c == null ? 0 : c;
    }

    private List<Long> ids(String sql) {
        return jdbc.queryForList(sql, Long.class);
    }
}
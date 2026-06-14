package com.ena.mam.seed;

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
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ReporterRepository reporterRepository;
    private final CameramanRepository cameramanRepository;
    private final LocationRepository locationRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final NewsRepository newsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final Random random = new Random(42);
    private final Faker faker = new Faker(random);

    public DataSeeder(
            ReporterRepository reporterRepository,
            CameramanRepository cameramanRepository,
            LocationRepository locationRepository,
            StaffMemberRepository staffMemberRepository,
            NewsRepository newsRepository
    ) {
        this.reporterRepository = reporterRepository;
        this.cameramanRepository = cameramanRepository;
        this.locationRepository = locationRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.newsRepository = newsRepository;
    }

    @Override
    public void run(String... args) {

        System.out.println("Starting optimized JPA seeding...");

        seedReporters();
        seedCameramen();
        seedLocations();
        seedStaffMembers();
        seedNews();

        System.out.println("Data seeding completed.");
    }

    private void seedReporters() {
        List<Reporter> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Reporter r = new Reporter();
            r.setReporterName(faker.name().fullName());
            r.setActive(true);
            list.add(r);
        }
        reporterRepository.saveAll(list);
        reporterRepository.flush();
        entityManager.clear();
    }

    private void seedCameramen() {
        List<Cameraman> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Cameraman c = new Cameraman();
            c.setCameramanName(faker.name().fullName());
            c.setActive(true);
            list.add(c);
        }
        cameramanRepository.saveAll(list);
        cameramanRepository.flush();
        entityManager.clear();
    }

    private void seedLocations() {
        List<Location> list = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Location l = new Location();
            l.setLocationName(faker.country().capital());
            l.setAbroad(random.nextBoolean());
            list.add(l);
        }
        locationRepository.saveAll(list);
        locationRepository.flush();
        entityManager.clear();
    }

    private void seedStaffMembers() {
        List<StaffMember> list = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            StaffMember s = new StaffMember();
            s.setStaffMemberName(faker.name().fullName());
            s.setActive(true);
            list.add(s);
        }
        staffMemberRepository.saveAll(list);
        staffMemberRepository.flush();
        entityManager.clear();
    }

    private void seedNews() {

        List<Reporter> reporters = reporterRepository.findAll();
        List<Cameraman> cameramen = cameramanRepository.findAll();
        List<Location> locations = locationRepository.findAll();
        List<StaffMember> staffMembers = staffMemberRepository.findAll();

        int totalRecords = 10_000; // scale to 1M later
        int batchSize = 1000;

        List<News> batch = new ArrayList<>(batchSize);

        for (int i = 0; i < totalRecords; i++) {

            News news = new News();

            news.setTitle(faker.lorem().sentence(3 + random.nextInt(12)));
            news.setNewsDate(LocalDate.now().minusDays(random.nextInt(3650)));
            news.setNumberOfFiles(1 + random.nextInt(100));
            news.setTotalSize(10 + random.nextDouble() * 5000);

            news.setImporter(staffMembers.get(random.nextInt(staffMembers.size())));
            news.setIngestor(staffMembers.get(random.nextInt(staffMembers.size())));

            news.setReporters(randomSubset(reporters, 10));
            news.setCameramen(randomSubset(cameramen, 10));
            news.setLocations(randomSubset(locations, 5));

            batch.add(news);

            if (batch.size() == batchSize) {

                newsRepository.saveAll(batch);

                newsRepository.flush();     // 🔥 key improvement
                entityManager.clear();      // 🔥 prevents memory explosion

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

    private <T> Set<T> randomSubset(List<T> list, int maxSize) {

        Set<T> result = new HashSet<>();

        int size = 1 + random.nextInt(maxSize);

        while (result.size() < size) {
            result.add(list.get(random.nextInt(list.size())));
        }

        return result;
    }
}
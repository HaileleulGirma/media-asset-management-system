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
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ReporterRepository reporterRepository;
    private final CameramanRepository cameramanRepository;
    private final LocationRepository locationRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final NewsRepository newsRepository;

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

        System.out.println("Starting data seeding...");

        seedReporters();
        seedCameramen();
        seedLocations();
        seedStaffMembers();
        seedNews();

        System.out.println("Data seeding completed.");
    }

    private void seedReporters() {

        List<Reporter> reporters = new ArrayList<>();

        for (int i = 0; i < 100; i++) {

            Reporter reporter = new Reporter();

            reporter.setReporterName(
                    faker.name().fullName()
            );

            reporter.setActive(true);

            reporters.add(reporter);
        }

        reporterRepository.saveAll(reporters);
    }

    private void seedCameramen() {

        List<Cameraman> cameramen = new ArrayList<>();

        for (int i = 0; i < 100; i++) {

            Cameraman cameraman = new Cameraman();

            cameraman.setCameramanName(
                    faker.name().fullName()
            );

            cameraman.setActive(true);

            cameramen.add(cameraman);
        }

        cameramanRepository.saveAll(cameramen);
    }

    private void seedLocations() {

        List<Location> locations = new ArrayList<>();

        for (int i = 0; i < 50; i++) {

            Location location = new Location();

            location.setLocationName(
                    faker.country().capital()
            );

            location.setAbroad(
                    random.nextBoolean()
            );

            locations.add(location);
        }

        locationRepository.saveAll(locations);
    }

    private void seedStaffMembers() {

        List<StaffMember> staffMembers = new ArrayList<>();

        for (int i = 0; i < 45; i++) {

            StaffMember staffMember = new StaffMember();

            staffMember.setStaffMemberName(
                    faker.name().fullName()
            );

            staffMember.setActive(true);

            staffMembers.add(staffMember);
        }

        staffMemberRepository.saveAll(staffMembers);
    }

    private void seedNews() {

        List<Reporter> reporters =
                reporterRepository.findAll();

        List<Cameraman> cameramen =
                cameramanRepository.findAll();

        List<Location> locations =
                locationRepository.findAll();

        List<StaffMember> staffMembers =
                staffMemberRepository.findAll();

        List<News> batch = new ArrayList<>();

        int totalRecords = 10_000; // change to 100_000 or 1_000_000 later
        int batchSize = 1000;

        for (int i = 0; i < totalRecords; i++) {

            News news = new News();

            news.setTitle(
                    faker.lorem().sentence(
                            3 + random.nextInt(12)
                    )
            );

            news.setNewsDate(
                    LocalDate.now()
                            .minusDays(
                                    random.nextInt(3650)
                            )
            );

            news.setNumberOfFiles(
                    1 + random.nextInt(100)
            );

            news.setTotalSize(
                    10 + random.nextDouble() * 5000
            );

            news.setReporters(
                    randomSubset(reporters, 10)
            );

            news.setCameramen(
                    randomSubset(cameramen, 10)
            );

            news.setLocations(
                    randomSubset(locations, 5)
            );

            news.setImporter(
                    staffMembers.get(
                            random.nextInt(
                                    staffMembers.size()
                            )
                    )
            );

            news.setIngestor(
                    staffMembers.get(
                            random.nextInt(
                                    staffMembers.size()
                            )
                    )
            );

            batch.add(news);

            if (batch.size() == batchSize) {

                newsRepository.saveAll(batch);

                batch.clear();

                System.out.println(
                        "Inserted "
                                + (i + 1)
                                + " news records"
                );
            }
        }

        if (!batch.isEmpty()) {
            newsRepository.saveAll(batch);
        }
    }

    private <T> HashSet<T> randomSubset(
            List<T> list,
            int maxSize
    ) {

        HashSet<T> result = new HashSet<>();

        int size = 1 + random.nextInt(maxSize);

        while (result.size() < size) {

            result.add(
                    list.get(
                            random.nextInt(
                                    list.size()
                            )
                    )
            );
        }

        return result;
    }
}
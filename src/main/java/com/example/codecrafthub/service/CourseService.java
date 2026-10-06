package com.example.codecrafthub.service;

import com.example.codecrafthub.model.Course;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for courses, plus reading and writing the courses.json file.
 *
 * Every operation follows the same simple pattern:
 *   1. read all courses from the file
 *   2. change the list in memory
 *   3. write the whole list back to the file
 *
 * The methods are "synchronized" so that two requests arriving at the same time
 * cannot read/write the file simultaneously and lose each other's changes.
 */
@Service
public class CourseService {

    /** The only status values a course may have (exact spelling and capitals). */
    public static final List<String> ALLOWED_STATUSES = List.of("Not Started", "In Progress", "Completed");

    // Jackson's JSON converter, provided automatically by Spring Boot.
    private final JsonMapper jsonMapper;

    // Location of the JSON file, e.g. "courses.json" in the folder the app is started from.
    private final Path dataFile;

    // Spring calls this constructor and passes in the values it needs ("dependency injection").
    public CourseService(JsonMapper jsonMapper,
                         @Value("${codecrafthub.data-file:courses.json}") String dataFilePath) {
        this.jsonMapper = jsonMapper;
        this.dataFile = Path.of(dataFilePath);
        createFileIfMissing();
    }

    // ---------------------------------------------------------------------
    // Public operations used by the controller
    // ---------------------------------------------------------------------

    public synchronized List<Course> getAllCourses() {
        return readCourses();
    }

    public synchronized Course getCourseById(long id) {
        return findById(readCourses(), id);
    }

    public synchronized Course addCourse(Course input) {
        validate(input);

        List<Course> courses = readCourses();

        Course course = new Course();
        course.setId(nextId(courses));
        copyEditableFields(input, course);
        course.setCreatedAt(Instant.now().toString());

        courses.add(course);
        writeCourses(courses);
        return course;
    }

    public synchronized Course updateCourse(long id, Course input) {
        validate(input);

        List<Course> courses = readCourses();
        Course existing = findById(courses, id);

        // id and created_at never change; everything else is replaced.
        copyEditableFields(input, existing);

        writeCourses(courses);
        return existing;
    }

    public synchronized void deleteCourse(long id) {
        List<Course> courses = readCourses();
        Course existing = findById(courses, id);
        courses.remove(existing);
        writeCourses(courses);
    }

    // ---------------------------------------------------------------------
    // Validation helpers
    // ---------------------------------------------------------------------

    /** Checks the request body and collects every problem so the client can fix them all at once. */
    private void validate(Course input) {
        if (input == null) {
            throw new InvalidCourseException(List.of("Request body is required"));
        }

        List<String> errors = new ArrayList<>();

        if (isBlank(input.getName())) {
            errors.add("name is required");
        }
        if (isBlank(input.getDescription())) {
            errors.add("description is required");
        }

        if (isBlank(input.getTargetDate())) {
            errors.add("target_date is required");
        } else {
            try {
                // LocalDate.parse only accepts real dates in the format YYYY-MM-DD.
                LocalDate.parse(input.getTargetDate());
            } catch (DateTimeParseException e) {
                errors.add("target_date must be a valid date in the format YYYY-MM-DD");
            }
        }

        if (isBlank(input.getStatus())) {
            errors.add("status is required");
        } else if (!ALLOWED_STATUSES.contains(input.getStatus())) {
            errors.add("status must be one of: " + String.join(", ", ALLOWED_STATUSES));
        }

        if (!errors.isEmpty()) {
            throw new InvalidCourseException(errors);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void copyEditableFields(Course from, Course to) {
        to.setName(from.getName().trim());
        to.setDescription(from.getDescription().trim());
        to.setTargetDate(from.getTargetDate());
        to.setStatus(from.getStatus());
    }

    private static Course findById(List<Course> courses, long id) {
        return courses.stream()
                .filter(course -> course.getId() == id)
                .findFirst()
                .orElseThrow(() -> new CourseNotFoundException(id));
    }

    /** Next id = highest existing id + 1, so the first course gets id 1. */
    private static long nextId(List<Course> courses) {
        return courses.stream()
                .mapToLong(Course::getId)
                .max()
                .orElse(0) + 1;
    }

    // ---------------------------------------------------------------------
    // File helpers
    // ---------------------------------------------------------------------

    /** Creates an empty courses.json ("[]") the first time the app runs. */
    private void createFileIfMissing() {
        try {
            if (Files.notExists(dataFile)) {
                Path parent = dataFile.toAbsolutePath().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(dataFile, "[]");
            }
        } catch (IOException e) {
            throw new StorageException("Could not create data file " + dataFile.toAbsolutePath(), e);
        }
    }

    private List<Course> readCourses() {
        createFileIfMissing(); // in case the file was deleted while the app was running
        try {
            // TypeReference tells Jackson we want a List of Course objects, not just "a List".
            return jsonMapper.readValue(dataFile.toFile(), new TypeReference<List<Course>>() {});
        } catch (JacksonException e) {
            throw new StorageException("Could not read courses from " + dataFile.toAbsolutePath(), e);
        }
    }

    private void writeCourses(List<Course> courses) {
        try {
            // Pretty printer = indented output, so the file is easy to read in an editor.
            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(dataFile.toFile(), courses);
        } catch (JacksonException e) {
            throw new StorageException("Could not save courses to " + dataFile.toAbsolutePath(), e);
        }
    }

    // ---------------------------------------------------------------------
    // Custom exceptions (the controller turns these into HTTP error responses)
    // ---------------------------------------------------------------------

    /** Thrown when no course has the requested id -> HTTP 404. */
    public static class CourseNotFoundException extends RuntimeException {
        public CourseNotFoundException(long id) {
            super("Course with id " + id + " not found");
        }
    }

    /** Thrown when the request body has missing or invalid fields -> HTTP 400. */
    public static class InvalidCourseException extends RuntimeException {
        private final List<String> errors;

        public InvalidCourseException(List<String> errors) {
            super("Invalid course data");
            this.errors = errors;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    /** Thrown when courses.json cannot be read or written -> HTTP 500. */
    public static class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package com.example.codecrafthub.controller;

import com.example.codecrafthub.model.Course;
import com.example.codecrafthub.service.CourseService;
import com.example.codecrafthub.service.CourseService.CourseNotFoundException;
import com.example.codecrafthub.service.CourseService.InvalidCourseException;
import com.example.codecrafthub.service.CourseService.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller: maps HTTP requests to Java methods.
 *
 * @RestController     -> return values are converted to JSON automatically (by Jackson).
 * @RequestMapping     -> every URL in this class starts with /api/courses.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private static final Logger log = LoggerFactory.getLogger(CourseController.class);

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    // POST /api/courses -> add a new course (returns 201 Created)
    @PostMapping
    public ResponseEntity<Course> addCourse(@RequestBody Course course) {
        Course created = courseService.addCourse(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /api/courses -> list all courses
    @GetMapping
    public List<Course> getAllCourses() {
        return courseService.getAllCourses();
    }

    // GET /api/courses/stats -> totals, e.g.
    // { "total_courses": 3, "by_status": { "Not Started": 1, "In Progress": 1, "Completed": 1 } }
    // Spring matches the fixed path "/stats" before the "/{id}" pattern, so the two don't clash.
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<Course> courses = courseService.getAllCourses();

        // Start every status at 0 so all three always appear, even when no course has them.
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String status : CourseService.ALLOWED_STATUSES) {
            byStatus.put(status, 0L);
        }
        for (Course course : courses) {
            byStatus.merge(course.getStatus(), 1L, Long::sum);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_courses", courses.size());
        stats.put("by_status", byStatus);
        return stats;
    }

    // GET /api/courses/{id} -> one course, e.g. /api/courses/1
    @GetMapping("/{id}")
    public Course getCourse(@PathVariable long id) {
        return courseService.getCourseById(id);
    }

    // PUT /api/courses/{id} -> replace all editable fields of a course
    @PutMapping("/{id}")
    public Course updateCourse(@PathVariable long id, @RequestBody Course course) {
        return courseService.updateCourse(id, course);
    }

    // DELETE /api/courses/{id} -> remove a course (returns 204 No Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // Error handling: each @ExceptionHandler turns an exception into a clear JSON error.
    // ---------------------------------------------------------------------

    // Missing fields, invalid status, bad date -> 400
    @ExceptionHandler(InvalidCourseException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCourse(InvalidCourseException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), e.getErrors());
    }

    // No course with that id -> 404
    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(CourseNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    // Body is empty or not valid JSON -> 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON", null);
    }

    // id in the URL is not a number, e.g. /api/courses/abc -> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleBadId(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "Course id must be a number", null);
    }

    // courses.json could not be read or written -> 500
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Map<String, Object>> handleStorage(StorageException e) {
        // Log the details for the developer, but don't expose file paths to API clients.
        log.error("Storage error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Could not access course data. Please try again later.", null);
    }

    /** Builds a consistent error body: { "status": 400, "error": "Bad Request", "message": "...", "details": [...] } */
    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message, List<String> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (details != null) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}

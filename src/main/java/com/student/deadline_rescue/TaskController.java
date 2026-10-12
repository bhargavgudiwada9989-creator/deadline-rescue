
package com.student.deadline_rescue;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TaskController {

    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // Display tasks, search, filters, and dashboard statistics
    @GetMapping("/tasks")
    public String listTasks(
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(required = false) String keyword,
            Model model) {

        List<Task> allTasks =
                taskRepository.findAllByOrderByDeadlineAsc();

        LocalDateTime now = LocalDateTime.now();

        // Dashboard statistics
        long totalTasks = allTasks.size();

        long completedTasks = allTasks.stream()
                .filter(Task::isCompleted)
                .count();

        long pendingTasks = allTasks.stream()
                .filter(task -> !task.isCompleted())
                .filter(task -> !task.getDeadline().isBefore(now))
                .count();

        long overdueTasks = allTasks.stream()
                .filter(task -> !task.isCompleted())
                .filter(task -> task.getDeadline().isBefore(now))
                .count();

        // Count unfinished tasks due within the next 24 hours
        long dueSoonTasks = allTasks.stream()
                .filter(task -> !task.isCompleted())
                .filter(task -> !task.getDeadline().isBefore(now))
                .filter(task -> !task.getDeadline()
                        .isAfter(now.plusHours(24)))
                .count();

        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("overdueTasks", overdueTasks);
        model.addAttribute("dueSoonTasks", dueSoonTasks);

        // Search and filter tasks
        List<Task> filteredTasks = allTasks.stream()
                .filter(task -> {
                    if (keyword == null || keyword.trim().isEmpty()) {
                        return true;
                    }

                    String searchText =
                            keyword.trim().toLowerCase();

                    boolean titleMatches =
                            task.getTitle() != null
                            && task.getTitle().toLowerCase()
                                    .contains(searchText);

                    boolean descriptionMatches =
                            task.getDescription() != null
                            && task.getDescription().toLowerCase()
                                    .contains(searchText);

                    return titleMatches || descriptionMatches;
                })
                .filter(task -> {
                    switch (filter.toLowerCase()) {
                        case "pending":
                            return !task.isCompleted()
                                    && !task.getDeadline().isBefore(now);

                        case "completed":
                            return task.isCompleted();

                        case "overdue":
                            return !task.isCompleted()
                                    && task.getDeadline().isBefore(now);
                        case "due-soon":
                            return !task.isCompleted()
                                    && !task.getDeadline().isBefore(now)
                                    && !task.getDeadline().isAfter(now.plusHours(24));
                        case "all":
                        default:
                            return true;
                    }
                })
                .toList();

        model.addAttribute("tasks", filteredTasks);
        model.addAttribute("selectedFilter", filter);
        model.addAttribute(
                "keyword", keyword == null ? "" : keyword);

        return "tasks";
    }

    // Add a new task
    @PostMapping("/tasks")
    public String addTask(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime deadline,
            @RequestParam(defaultValue = "MEDIUM") String priority) {

        if (title == null || title.trim().isEmpty()) {
            return "redirect:/tasks";
        }

        Task task = new Task();
        task.setTitle(title.trim());
        task.setDescription(
                description == null ? "" : description.trim());
        task.setDeadline(deadline);
        task.setPriority(priority);
        task.setCompleted(false);

        taskRepository.save(task);

        return "redirect:/tasks";
    }

    // Mark a task complete or reopen it
    @PostMapping("/tasks/{id}/complete")
    public String toggleComplete(@PathVariable Long id) {

        taskRepository.findById(id).ifPresent(task -> {
            task.setCompleted(!task.isCompleted());
            taskRepository.save(task);
        });

        return "redirect:/tasks";
    }

    // Delete a task
    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id) {

        taskRepository.findById(id).ifPresent(taskRepository::delete);

        return "redirect:/tasks";
    }

    // Display the edit form
    @GetMapping("/tasks/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model) {

        return taskRepository.findById(id)
                .map(task -> {
                    model.addAttribute("task", task);
                    return "edit-task";
                })
                .orElse("redirect:/tasks");
    }

    // Save an edited task
    @PostMapping("/tasks/{id}/edit")
    public String updateTask(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime deadline,
            @RequestParam(defaultValue = "MEDIUM") String priority) {

        if (title == null || title.trim().isEmpty()) {
            return "redirect:/tasks";
        }

        return taskRepository.findById(id)
                .map(task -> {
                    task.setTitle(title.trim());
                    task.setDescription(
                            description == null ? "" : description.trim());
                    task.setDeadline(deadline);
                    task.setPriority(priority);

                    taskRepository.save(task);

                    return "redirect:/tasks";
                })
                .orElse("redirect:/tasks");
    }
}

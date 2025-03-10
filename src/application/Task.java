package application;

import java.time.LocalDateTime;
import java.util.Objects;

public class Task {
    private int id;
    private String title;
    private String description;
    private LocalDateTime dueDateTime;
    private String repeatType;
    private boolean completed;
    
    
    // Constructor
    public Task(int id, String title, String description, LocalDateTime dueDateTime, String repeatType, boolean completed) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDateTime = dueDateTime;
        this.repeatType = repeatType;
        this.completed = completed;
    }
    
    // Getter und Setter
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getDueDateTime() {
        return dueDateTime;
    }
    
    public void setDueDateTime(LocalDateTime dueDateTime) {
        this.dueDateTime = dueDateTime;
    }
    
    public String getRepeatType() {
        return repeatType;
    }
    
    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
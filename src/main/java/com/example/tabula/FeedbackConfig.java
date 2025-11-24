package com.example.tabula;

import java.util.List;

public class FeedbackConfig {
    private GlobalSettings global;
    private List<FileConfig> files;

    public GlobalSettings getGlobal() {
        return global;
    }

    public void setGlobal(GlobalSettings global) {
        this.global = global;
    }

    public List<FileConfig> getFiles() {
        return files;
    }

    public void setFiles(List<FileConfig> files) {
        this.files = files;
    }

    public static class GlobalSettings {
        private boolean guess_columns;
        private boolean lattice_mode;
        private boolean stream_mode;

        public boolean isGuess_columns() {
            return guess_columns;
        }

        public void setGuess_columns(boolean guess_columns) {
            this.guess_columns = guess_columns;
        }

        public boolean isLattice_mode() {
            return lattice_mode;
        }

        public void setLattice_mode(boolean lattice_mode) {
            this.lattice_mode = lattice_mode;
        }

        public boolean isStream_mode() {
            return stream_mode;
        }

        public void setStream_mode(boolean stream_mode) {
            this.stream_mode = stream_mode;
        }
    }

    public static class FileConfig {
        private String filename;
        private List<Issue> issues;
        private Overrides overrides;

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public List<Issue> getIssues() {
            return issues;
        }

        public void setIssues(List<Issue> issues) {
            this.issues = issues;
        }

        public Overrides getOverrides() {
            return overrides;
        }

        public void setOverrides(Overrides overrides) {
            this.overrides = overrides;
        }
    }

    public static class Issue {
        private String type;
        private String description;
        private int page;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }
    }

    public static class Overrides {
        private List<Float> area;
        private List<Float> columns;

        public List<Float> getArea() {
            return area;
        }

        public void setArea(List<Float> area) {
            this.area = area;
        }

        public List<Float> getColumns() {
            return columns;
        }

        public void setColumns(List<Float> columns) {
            this.columns = columns;
        }
    }
}

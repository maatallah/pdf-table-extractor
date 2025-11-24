package com.example.tabula;

import java.util.List;

public class TableData {
    private List<List<String>> rows;
    private int pageNumber;

    public TableData(List<List<String>> rows, int pageNumber) {
        this.rows = rows;
        this.pageNumber = pageNumber;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public void setRows(List<List<String>> rows) {
        this.rows = rows;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
}

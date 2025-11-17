package com.example.stock_jules;

class ExportRecord {
    public int id;
    public String filename;
    public String filepath;
    public String exportTime;
    public int batchId;
    public String format;
    public boolean isFullExport;

    public ExportRecord(int id, String filename, String filepath, String exportTime,
                        int batchId, String format, boolean isFullExport) {
        this.id = id;
        this.filename = filename;
        this.filepath = filepath;
        this.exportTime = exportTime;
        this.batchId = batchId;
        this.format = format;
        this.isFullExport = isFullExport;
    }
}
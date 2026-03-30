package com.vicente.taskmanager.domain.enums;

import java.util.Arrays;

public enum FileExtension {
    PDF("pdf"),
    DOCX("docx"),
    DOC("doc"),
    XLSX("xlsx"),
    XLS("xls"),
    PPTX("pptx"),
    PPT("ppt"),
    TXT("txt"),
    ZIP("zip"),
    JPG("jpg"),
    JPEG("jpeg"),
    PNG("png"),
    SEVEN_Z("7z"),
    RAR("rar");

    private final String extension;

    FileExtension(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    /**
     * Verifica se a extensão fornecida está na lista de extensões permitidas.
     * <p>
     * Este método percorre todos os valores do enum `FileExtension` e compara
     * cada extensão (ignorando maiúsculas/minúsculas) com a extensão fornecida.
     *
     * @param ext The file extension to be checked (e.g., "pdf", "jpg").
     * @return true if the extension is among the allowed ones; false otherwise.
     */
    public static boolean isAllowedExtension(String ext) {
        return Arrays.stream(values()).anyMatch(e ->
                e.getExtension().equalsIgnoreCase(ext));
    }
}

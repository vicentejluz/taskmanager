package com.vicente.taskmanager.domain.enums;

import java.util.Arrays;

public enum FileExtension {
    PDF("pdf", true),
    DOCX("docx", false),
    DOC("doc", false),
    XLSX("xlsx", false),
    XLS("xls", false),
    PPTX("pptx", false),
    PPT("ppt", false),
    TXT("txt", false),
    ZIP("zip", false),
    JPG("jpg", true),
    JPEG("jpeg", true),
    PNG("png", true),
    SEVEN_Z("7z", false),
    RAR("rar", false);

    private final String extension;
    private final boolean inline;

    FileExtension(String extension, boolean inline) {
        this.extension = extension;
        this.inline = inline;
    }

    public String getExtension() {
        return extension;
    }

    public boolean isInline() {
        return inline;
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

    /**
     * Retorna a constante do enum {@link FileExtension} que corresponde à extensão de arquivo fornecida.
     * <p>
     * Este método percorre todos os valores do enum {@code FileExtension} e compara
     * cada extensão (ignorando maiúsculas/minúsculas) com o valor fornecido.
     * Se uma correspondência for encontrada, a constante do enum correspondente é retornada.
     * Caso contrário, uma {@link IllegalArgumentException} é lançada.
     *
     * @param ext The file extension to resolve (e.g., "pdf", "jpg").
     * @return The corresponding {@code FileExtension} enum constant.
     * @throws IllegalArgumentException If the provided extension is not supported.
     */
    public static FileExtension fromExtension(String ext) {
        return Arrays.stream(values()).filter(e ->
                e.getExtension().equalsIgnoreCase(ext)).findFirst().orElseThrow(() ->
                new IllegalArgumentException("Invalid extension: " + ext));
    }
}

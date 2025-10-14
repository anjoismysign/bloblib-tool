package io.github.anjoismysign.bloblibtool;

import com.intellij.openapi.ide.CopyPasteManager;

import java.awt.datatransfer.StringSelection;

public final class ClipboardUtils {
    private ClipboardUtils() {}

    public static void copyToClipboard(String s) {
        CopyPasteManager.getInstance().setContents(new StringSelection(s));
    }
}

package io.github.anjoismysign.bloblibtool;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class GeneratorDialog extends DialogWrapper {

    private final JTextArea inputArea = new JTextArea(4, 40);

    public GeneratorDialog() {
        super(true);
        setTitle("Generate DataAsset Record");
        init();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel("Enter definition (ClassName: type name, ...):"), BorderLayout.NORTH);
        panel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        JPanel hint = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hint.add(new JLabel("Example: Person:boolean isMale,Passport passport"));
        panel.add(hint, BorderLayout.SOUTH);
        return panel;
    }

    public String getDefinitionInput() {
        return inputArea.getText();
    }
}

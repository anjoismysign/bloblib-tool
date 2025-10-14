package io.github.anjoismysign.bloblibtool;

import com.intellij.ide.IdeView;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;

public class GenerateAssetAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            Messages.showErrorDialog("No project found.", "Asset Generator");
            return;
        }

        IdeView ideView = e.getData(LangDataKeys.IDE_VIEW);
        if (ideView == null) {
            Messages.showErrorDialog(project, "Unable to determine target directory. Open the Project/Package view and try again.", "Asset Generator");
            return;
        }

        PsiDirectory targetDir = ideView.getOrChooseDirectory();
        if (targetDir == null) {
            Messages.showErrorDialog(project, "No directory selected.", "Asset Generator");
            return;
        }

        GeneratorDialog dialog = new GeneratorDialog();
        boolean ok = dialog.showAndGet();
        if (!ok) return;

        String input = dialog.getDefinitionInput();
        if (input == null || input.trim().isEmpty()) {
            Messages.showErrorDialog(project, "Definition is empty.", "Asset Generator");
            return;
        }

        // Extract the class name (part before ':') for the filename
        String className;
        try {
            String[] parts = input.trim().split(":", 2);
            if (parts.length != 2 || parts[0].trim().isEmpty()) {
                throw new IllegalArgumentException("Definition must start with the class name followed by ':'. Example: Person:int age,boolean isMale");
            }
            className = parts[0].trim();
        } catch (RuntimeException ex) {
            Messages.showErrorDialog(project, "Invalid definition: " + ex.getMessage(), "Asset Generator");
            return;
        }

        // Generate the source (this will validate the rest and possibly throw our custom exceptions)
        final String generatedSource;
        try {
            generatedSource = CodeGenerator.generateSource(input);
        } catch (RuntimeException ex) {
            Messages.showErrorDialog(project, "Failed to generate source: " + ex.getMessage(), "Asset Generator");
            return;
        }

        final String fileName = className + ".java";

        // Check if file already exists in the target directory
        PsiFile existing = targetDir.findFile(fileName);
        if (existing != null) {
            int res = Messages.showYesNoDialog(
                    project,
                    "A file named '" + fileName + "' already exists in " + targetDir.getVirtualFile().getPath() + ".\nDo you want to overwrite it?",
                    "Overwrite File?",
                    Messages.getQuestionIcon()
            );
            if (res != Messages.YES) {
                Messages.showInfoMessage(project, "Generation cancelled.", "Asset Generator");
                return;
            }
        }

        // Create the PSI file from the generated text and add it to the directory inside a write action
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiFile psiFile = PsiFileFactory.getInstance(project)
                    .createFileFromText(fileName, JavaLanguage.INSTANCE, generatedSource);

            // If file exists, delete first (we confirmed overwrite above)
            PsiFile present = targetDir.findFile(fileName);
            try {
                if (present != null) {
                    present.delete();
                }
                PsiFile added = (PsiFile) targetDir.add(psiFile);

                // Open the newly created file in an editor tab
                if (added.getVirtualFile() != null) {
                    FileEditorManager.getInstance(project).openFile(added.getVirtualFile(), true);
                }
            } catch (Exception ex) {
                // Any failure during write (e.g., VFS), show a message to the user
                Messages.showErrorDialog(project, "Failed to create file: " + ex.getMessage(), "Asset Generator");
            }
        });

        Messages.showInfoMessage(project, "Generated " + fileName + " in " + targetDir.getVirtualFile().getPath(), "Asset Generator");
    }

    @Override
    public void update(AnActionEvent e) {
        // Enable this action only when there is a project and an IdeView (so it appears in New -> ... when appropriate)
        boolean enabled = e.getProject() != null && e.getData(LangDataKeys.IDE_VIEW) != null;
        e.getPresentation().setEnabledAndVisible(enabled);
    }
}
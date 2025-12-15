package io.github.lemon_ant.jharmonizer.core.files_handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.SrcFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceFilesHandlerTest {

    @TempDir
    Path tempDir;

    @Test
    void backup_existingFile_renamedWithBakExtension() throws IOException {
        Path sourceFile = Files.writeString(tempDir.resolve("Example.java"), "class Example {}");

        SourceFilesHandler.renameToBackup(sourceFile);

        Path expectedBackup = tempDir.resolve("Example.java.bak");
        assertThat(expectedBackup).exists();
        assertThat(sourceFile).doesNotExist();
    }

    @Test
    void backup_nonExistingFile_throwIOException() {
        Path missingFile = tempDir.resolve("DoesNotExist.java");

        assertThatThrownBy(() -> SourceFilesHandler.renameToBackup(missingFile))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void fileContent_twoInstancesWithSameValues_equalsAndHashCodeMatch() {
        Path path1 = tempDir.resolve("A.java");
        Path path2 = tempDir.resolve("B.java");

        SrcFile fc1a = new SrcFile("x", path1);
        SrcFile fc1b = new SrcFile("x", path1);
        SrcFile fc2 = new SrcFile("y", path2);

        assertThat(fc1a).isEqualTo(fc1b);
        assertThat(fc1a.hashCode()).isEqualTo(fc1b.hashCode());

        assertThat(fc1a).isNotEqualTo(fc2);
    }

    @Test
    void findJavaFiles_validRequest_returnMatchingFiles() throws IOException {
        Path javaFile = Files.writeString(tempDir.resolve("MyClass.java"), "class MyClass {}");
        Path txtFile = Files.writeString(tempDir.resolve("notes.txt"), "not java");

        List<Path> result = SourceFilesHandler.findJavaFiles(tempDir, Set.of("**.java"), Set.of())
                .collect(Collectors.toList());

        assertThat(result)
                .contains(javaFile.normalize().toAbsolutePath())
                .doesNotContain(txtFile.normalize().toAbsolutePath());
    }

    @Test
    void overwrite_Unchecked_existingFile_replaceFileContent() throws IOException {
        Path sourceFile = Files.writeString(tempDir.resolve("Overwrite.java"), "old content");
        SrcFile srcFile = new SrcFile("new content", sourceFile);

        SourceFilesHandler.overwrite(srcFile.getPath(), srcFile.getSrcCode());

        String newText = Files.readString(sourceFile);
        assertThat(newText).isEqualTo("new content");
    }

    @Test
    void readFile_existingFile_returnFileContent() throws IOException {
        Path file = Files.writeString(tempDir.resolve("ReadMe.java"), "class R {}");

        SrcFile content = new SrcFile(Files.readString(file, StandardCharsets.UTF_8), file);

        assertThat(content.getPath()).isEqualTo(file);
        assertThat(content.getSrcCode()).isEqualTo("class R {}");
    }
}

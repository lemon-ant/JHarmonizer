package io.github.lemon_ant.jharmonizer.core.files_handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
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
        // Given
        Path sourceFile = Files.writeString(tempDir.resolve("Example.java"), "class Example {}");

        // When
        SourceFilesHandler.renameToBackup(sourceFile);

        // Then
        Path expectedBackup = tempDir.resolve("Example.java.bak");
        assertThat(expectedBackup).exists();
        assertThat(sourceFile).doesNotExist();
    }

    @Test
    void backup_nonExistingFile_throwsIOException() {
        // Given
        Path missingFile = tempDir.resolve("DoesNotExist.java");

        // When / Then
        assertThatThrownBy(() -> SourceFilesHandler.renameToBackup(missingFile))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void fileContent_twoInstancesWithSameValues_equalsAndHashCodeMatch() {
        // Given
        Path path1 = tempDir.resolve("A.java");
        Path path2 = tempDir.resolve("B.java");

        SrcFile fc1a = new SrcFile("x", path1);
        SrcFile fc1b = new SrcFile("x", path1);
        SrcFile fc2 = new SrcFile("y", path2);

        // When / Then
        assertThat(fc1a).isEqualTo(fc1b);
        assertThat(fc1a.hashCode()).isEqualTo(fc1b.hashCode());
        assertThat(fc1a).isNotEqualTo(fc2);
    }

    @Test
    void findJavaFiles_validRequest_returnsMatchingFiles() throws IOException {
        // Given
        Path javaFile = Files.writeString(tempDir.resolve("MyClass.java"), "class MyClass {}");
        Path txtFile = Files.writeString(tempDir.resolve("notes.txt"), "not java");

        // When
        List<Path> result = SourceFilesHandler.findJavaFiles(tempDir, Set.of("**.java"), Set.of())
                .collect(Collectors.toList());

        // Then
        assertThat(result)
                .contains(javaFile.normalize().toAbsolutePath())
                .doesNotContain(txtFile.normalize().toAbsolutePath());
    }

    @Test
    void overwrite_existingFile_replacesFileContent() throws IOException {
        // Given
        Path sourceFile = Files.writeString(tempDir.resolve("Overwrite.java"), "old content");
        SrcFile srcFile = new SrcFile("new content", sourceFile);

        // When
        SourceFilesHandler.overwrite(srcFile.getPath(), srcFile.getSrcCode());

        // Then
        String newText = Files.readString(sourceFile);
        assertThat(newText).isEqualTo("new content");
    }

    @Test
    void readFile_existingFile_returnsFileContent() throws IOException {
        // Given
        Path file = Files.writeString(tempDir.resolve("ReadMe.java"), "class R {}");

        // When
        SrcFile content = SourceFilesHandler.readJavaFiles(tempDir, Set.of("ReadMe.java"), Set.of())
                .findFirst()
                .orElseThrow();

        // Then
        assertThat(content.getPath()).isEqualTo(file);
        assertThat(content.getSrcCode()).isEqualTo("class R {}");
    }
}

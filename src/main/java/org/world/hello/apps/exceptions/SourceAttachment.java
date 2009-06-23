package org.world.hello.apps.exceptions;

import java.util.List;

/**
 * Exception has source attachment
 */
public interface SourceAttachment {

    String getSourceFile();
    List<String> getSource();
    Integer getLineNumber();
}

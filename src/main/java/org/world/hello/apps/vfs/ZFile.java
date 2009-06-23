package org.world.hello.apps.vfs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.world.hello.apps.exceptions.UnexpectedException;

/**
 * A ZIP implementation
 */
public class ZFile extends VirtualFile {

    public ZipFile zip;
    public List<ZFile> childs = new ArrayList<ZFile>();
    public ZipEntry entry = null;
    public String name = "";
    public String fullPath = "";

    public ZFile(File fl) throws IOException {
        zip = new ZipFile(fl);
        Enumeration entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry lentry = (ZipEntry) entries.nextElement();
            ZFile zf = getOrCreate(lentry.getName());
            zf.zip = zip;
            zf.entry = lentry;
            zf.fullPath = lentry.getName();
        }
    }

    private ZFile getOrCreate(String name) {
        String tmp = name;
        if (tmp.endsWith("/")) {
            tmp = tmp.substring(0, tmp.length() - 1);
        }
        String[] path = name.split("/");
        ZFile root = this;
        for (int i = 0; i < path.length; i++) {
            String part = path[i];
            ZFile newRoot = null;
            for (ZFile fl : root.childs) {
                if (fl.name.equals(part)) {
                    newRoot = fl;
                    break;
                }
            }
            if (newRoot != null) {
                root = newRoot;
            } else {
                ZFile zf = new ZFile(zip);
                zf.name = part;
                root.childs.add(zf);
                root = zf;
            }
        }
        return root;
    }

    private ZFile(ZipFile zip) {
        this.zip = zip;
    }

    public VirtualFile child(String name) {
        String tmp = name;
        if (tmp.endsWith("/")) {
            tmp = tmp.substring(0, tmp.length() - 1);
        }
        String[] path = name.split("/");
        ZFile root = this;
        for (int i = 0; i < path.length; i++) {
            String part = path[i];
            ZFile newRoot = null;
            for (ZFile fl : root.childs) {
                if (fl.name.equals(part)) {
                    newRoot = fl;
                    break;
                }
            }
            if (newRoot != null) {
                root = newRoot;
            } else {
                return new ZFile(zip);
            }
        }
        return root;
    }

    public boolean exists() {
        return entry != null ? true : false;
    }

    public String getName() {
        return name;
    }

    public InputStream inputstream() {
        try {
            return zip.getInputStream(entry);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    public OutputStream outputstream() {
        throw new UnsupportedOperationException("Not supported for ZFile.");
    }

    public boolean isDirectory() {
        return entry != null ? entry.isDirectory() : true;
    }

    public Long lastModified() {
        return 0L;
    }

    public long length() {
        return (entry != null && !entry.isDirectory()) ? entry.getSize() : -1;
    }

    @SuppressWarnings("unchecked")
    public List<VirtualFile> list() {
        return (List) childs;
    }

    public String relativePath() {
        return fullPath;
    }

    public Channel channel() {
        return Channels.newChannel(inputstream());
    }
}

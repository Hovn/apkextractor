package com.github.ghmxr.apkextractor.items;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.ghmxr.apkextractor.utils.EnvironmentUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class StandardFileItem extends FileItem {
    private File file;

    protected StandardFileItem(String path) {
        this(new File(path));
    }

    protected StandardFileItem(File file) {
        this.file = file;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public boolean isFile() {
        return file.isFile();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public boolean renameTo(@NonNull String newName0) throws Exception {
        //如果新文件名中存在不被允许的字符,win（\/:*?"<>|），lin(:等),替换掉非法字符
        String newName=newName0.replace(":","-");
        final File destFile = new File(file.getParentFile(), newName);

        //如果文件名未更改
        if(getName().equals(newName)){
            //throw new Exception(newName + " filename unchanged, ignore!");
            return true;
        }

        //如果目标文件已经存在，则加上随机后缀
        if (destFile.exists()) {
            //throw new Exception(destFile.getAbsolutePath() + " already exists!");
            String main= EnvironmentUtil.getFileMainName(newName);
            int random= new Random().nextInt(100);//[0,100)
            String ext= EnvironmentUtil.getFileExtensionName(newName);
            String newNewName= main+"_e"+random+"."+ext;
            return renameTo(newNewName);
        }

        //实际重命名
        if (file.renameTo(destFile)) {
            file = destFile;
            return true;
        } else {
            //return false;
            throw new Exception("error renaming to " + destFile.getAbsolutePath());
        }
    }

    @Override
    public boolean canGetRealPath() {
        return true;
    }

    @Override
    public String getPath() {
        return file.getAbsolutePath();
    }

    @Override
    public boolean delete() {
        return file.delete();
    }

    @NonNull
    @Override
    public List<FileItem> listFileItems() {
        ArrayList<FileItem> arrayList = new ArrayList<>();
        try {
            File[] files = file.listFiles();
            if (files == null) return arrayList;
            for (File f : files) arrayList.add(new StandardFileItem(f));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrayList;
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Nullable
    @Override
    public FileItem getParent() {
        File parent = file.getParentFile();
        if (parent != null) {
            return new StandardFileItem(parent);
        }
        return null;
    }

    @Override
    public InputStream getInputStream() throws Exception {
        return new FileInputStream(file);
    }

    @Override
    public OutputStream getOutputStream() throws Exception {
        return new FileOutputStream(file);
    }

    @Override
    public boolean isFileInstance() {
        return true;
    }

    @Override
    public boolean mkdirs() {
        return file.mkdirs();
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public boolean isHidden() {
        return file.isHidden();
    }

    @NonNull
    @Override
    public String toString() {
        return file.getAbsolutePath();
    }
}

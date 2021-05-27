package de.embl.cba.mobie2;

import ij.gui.GenericDialog;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import static de.embl.cba.tables.FileUtils.resolveTableURL;
import static de.embl.cba.tables.S3Utils.getS3FileNames;
import static de.embl.cba.tables.S3Utils.selectS3PathFromDirectory;
import static de.embl.cba.tables.github.GitHubUtils.getFileNames;
import static de.embl.cba.tables.github.GitHubUtils.selectGitHubPathFromDirectory;

public class PathHelpers {

    public enum FileLocation {
        Project,
        FileSystem
    }

    public static boolean isGithub( String directory ) {
        return directory.contains( "raw.githubusercontent" );
    }

    public static boolean isS3( String directory ) {
        return directory.contains( "s3.amazon.aws.com" ) || directory.startsWith("https://s3");
    }

    // objectName is used for the dialog labels e.g. 'table', 'bookmark' etc...
    public static String selectPathFromProjectOrFileSystem ( String directory, String objectName ) throws IOException {
        FileLocation fileLocation = null;
        if ( directory != null )
        {
            final GenericDialog gd = new GenericDialog( "Choose source" );
            gd.addChoice( "Load from", new String[]{ FileLocation.Project.toString(),
                    FileLocation.FileSystem.toString() }, FileLocation.Project.toString() );
            gd.showDialog();
            if ( gd.wasCanceled() ) return null;
            fileLocation = FileLocation.valueOf( gd.getNextChoice() );
        }

        String filePath = null;
        if ( directory != null && fileLocation.equals( FileLocation.Project ) && isGithub( directory ) )
        {
            // we choose from project and have a github project
            filePath = selectGitHubPathFromDirectory( directory, objectName );
            if ( filePath == null ) return null;
        }
        else if ( directory != null && fileLocation.equals( FileLocation.Project ) && isS3( directory ) )
        {
            // we choose from project and have a s3 project
            filePath = selectS3PathFromDirectory(directory, objectName);
            if ( filePath == null ) return null;
        }
        else
        {
            final JFileChooser jFileChooser = new JFileChooser( directory );

            if ( jFileChooser.showOpenDialog( null ) == JFileChooser.APPROVE_OPTION )
                filePath = jFileChooser.getSelectedFile().getAbsolutePath();
        }

        if ( filePath == null ) return null;

        if ( filePath.startsWith( "http" ) )
            filePath = resolveTableURL( URI.create( filePath ) );

        return filePath;
    }

    public static String[] getFileNamesFromProject( String directory ) {
        if ( directory != null ) {
            if ( isGithub(directory) ) {
                return getFileNames( directory );
            } else if ( isS3(directory)) {
                return getS3FileNames( directory );
            } else {
                File[] files = new File(directory).listFiles();
                if ( files != null ) {
                    String[] fileNames = new String[files.length];
                    for ( int i = 0; i< files.length; i++) {
                        fileNames[i] = files[i].getName();
                    }
                    return fileNames;
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

}

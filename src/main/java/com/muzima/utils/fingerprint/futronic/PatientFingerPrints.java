/*
 * DbRecord.java
 */

package com.muzima.utils.fingerprint.futronic;

import com.futronictech.SDKHelper.FtrIdentifyRecord;
import com.muzima.api.model.Patient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Vector;

/**
 * This class represent a patients fingerprint records.
 */
public class PatientFingerPrints {

    /**
     * User unique key
     */
    private byte[] m_Key;
    /**
     * Patients
     */
    private Patient patient;
    /**
     * Finger template.
     */
    private byte[] fingerTemplate;

    /**
     * Creates a new instance of PatientFingerPrints class.
     */
    public PatientFingerPrints() {
        // Generate user's unique identifier
        m_Key = new byte[16];
        java.util.UUID guid = java.util.UUID.randomUUID();
        long itemHigh = guid.getMostSignificantBits();
        long itemLow = guid.getLeastSignificantBits();
        for (int i = 7; i >= 0; i--) {
            m_Key[i] = (byte) (itemHigh & 0xFF);
            itemHigh >>>= 8;
            m_Key[8 + i] = (byte) (itemLow & 0xFF);
            itemLow >>>= 8;
        }
        fingerTemplate = null;
    }

    /**
     * Initialize a new instance of PatientFingerPrints class from the file.
     *
     * @param szFileName a file name with previous saved passport.
     */
    public PatientFingerPrints(String szFileName)
            throws FileNotFoundException, NullPointerException, AppException {
        load(szFileName);
    }

    /**
     * Function read all records from database.
     *
     * @param szDbDir database folder
     * @return reference to Vector objects with records
     */
    static Vector<PatientFingerPrints> readRecords(String szDbDir) {
        File DbDir;
        File[] files;
        Vector<PatientFingerPrints> Users = new Vector<PatientFingerPrints>(10, 10);
        // Read all records to identify
        DbDir = new File(szDbDir);
        files = DbDir.listFiles();
        if ((files == null) || (files.length == 0)) {
            return Users;
        }
        for (int iFiles = 0; iFiles < files.length; iFiles++) {
            try {
                if (files[iFiles].isFile()) {
                    PatientFingerPrints User = new PatientFingerPrints(files[iFiles].getAbsolutePath());
                    Users.add(User);
                }
            } catch (FileNotFoundException e) {
                // The record has invalid data. Skip it and continue processing.
            } catch (NullPointerException e) {
                // The record has invalid data. Skip it and continue processing.
            } catch (AppException e) {
                // The record has invalid data or access denied. Skip it and continue processing.
            }
        }
        return Users;
    }

    /**
     * load user's information from file.
     *
     * @param szFileName a file name with previous saved passport.
     * @throws NullPointerException           szFileName parameter has null reference.
     * @throws java.io.InvalidObjectException the file has invalid structure.
     * @throws java.io.FileNotFoundException  the file not found or access denied.
     */
    private void load(String szFileName)
            throws FileNotFoundException, NullPointerException, AppException {
        FileInputStream fs = null;
        File f = null;
        long nFileSize;
        f = new File(szFileName);
        if (!f.exists() || !f.canRead())
            throw new FileNotFoundException("File " + f.getPath());
        try {
            nFileSize = f.length();
            fs = new FileInputStream(f);
            CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder();
            byte[] Data = null;
            // Read user name length and user name in UTF8
            if (nFileSize < 2) {
                fs.close();
                throw new AppException("Bad file " + f.getPath());
            }
            int nLength = (fs.read() << 8) | fs.read();
            nFileSize -= 2;
            if (nFileSize < nLength) {
                fs.close();
                throw new AppException("Bad file " + f.getPath());
            }
            nFileSize -= nLength;
            Data = new byte[nLength];
            fs.read(Data);
            // Read user unique ID
            if (nFileSize < 16) {
                fs.close();
                throw new AppException("Bad file " + f.getPath());
            }
            nFileSize -= 16;
            m_Key = new byte[16];
            fs.read(m_Key);
            // Read template length and template data
            if (nFileSize < 2) {
                fs.close();
                throw new AppException("Bad file " + f.getPath());
            }
            nLength = (fs.read() << 8) | fs.read();
            nFileSize -= 2;
            if (nFileSize != nLength) {
                fs.close();
                throw new AppException("Bad file " + f.getPath());
            }
            fingerTemplate = new byte[nLength];
            fs.read(fingerTemplate);
            fs.close();
        } catch (SecurityException e) {
            throw new AppException("Denies read access to the file " + szFileName);
        } catch (IOException e) {
            throw new AppException("Bad file " + szFileName);
        }
    }

    /**
     * save user's information to file.
     *
     * @param szFileName a file name to save.
     * @return true if passport successfully saved to file, otherwise false.
     * @throws NullPointerException  szFileName parameter has null reference.
     * @throws IllegalStateException some parameters are not set.
     * @throws java.io.IOException   can not create file or can not write data into file.
     */
    public boolean save(String szFileName)
            throws NullPointerException, IllegalStateException, IOException {
        FileOutputStream fs = null;
        File f = null;
        boolean bRetcode = false;
        try {
            f = new File(szFileName);
            fs = new FileOutputStream(f);
            CharsetEncoder utf8Encoder = Charset.forName("UTF-8").newEncoder();
            byte[] Data = null;
            // save user unique ID
            fs.write(m_Key);
            // save user template
            fs.write(((fingerTemplate.length >>> 8) & 0xFF));
            fs.write((fingerTemplate.length & 0xFF));
            fs.write(fingerTemplate);
            fs.close();
            bRetcode = true;
        } finally {
            if (!bRetcode && f != null)
                f.delete();
        }
        return bRetcode;
    }

    /**
     * Get the user template.
     */
    public byte[] getTemplate() {
        return fingerTemplate;
    }

    /**
     * Set the user template.
     */
    public void setTemplate(byte[] value) {
        fingerTemplate = value;
    }

    /**
     * Get the user unique identifier.
     */
    public byte[] getUniqueID() {
        return m_Key;
    }

    public FtrIdentifyRecord getFtrIdentifyRecord() {
        FtrIdentifyRecord r = new FtrIdentifyRecord();
        r.m_KeyValue = m_Key;
        r.m_Template = fingerTemplate;
        return r;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }
}

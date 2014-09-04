/*
 * DbRecord.java
 */

package com.muzima.utils.fingerprint.futronic;

import android.util.Base64;
import com.futronictech.SDKHelper.FtrIdentifyRecord;
import com.muzima.api.model.Patient;

import java.io.IOException;

/**
 * This class represent a user fingerprint database record.
 *
 */
public class FingerprintRecord    extends Patient
{

	//[uuid, name, finger, age, identifier];
	private String identifier,name,lname,fname,sname, age,day,month,year, uuid,amrsid,nextvisitDate,patientId,phone,finger,fingername,phone2,duration,location,receive,condition;
	
	
	   
               
  

    public String getLname()
    {
        return lname;
    }

    public void setLname( String lname )
    {
        this.lname = lname;
    }

    public String getFname()
    {
        return fname;
    }

    public void setFname( String fname )
    {
        this.fname = fname;
    }

    public String getSname()
    {
        return sname;
    }

    public void setSname( String sname )
    {
        this.sname = sname;
    }

    /**
     * User unique key
     */
    private byte[] m_Key;

    /**
     * Finger template.
     */
    private byte[] m_Template;
    
    /**
     * Creates a new instance of PatientsFingerprints class.
     * @throws java.io.IOException
     */
    public FingerprintRecord(String uuid, String temp) throws IOException
    {
        // Generate user's unique identifier
        m_Key = uuid.getBytes();
        m_Template = Base64.decode(temp,Base64.DEFAULT);
    }

    
    public String getIdentifier() {
        return identifier;
}

public void setIdentifier(String identifier) {
        this.identifier = identifier;
}
    /**
     * Get the user template.
     */
    public byte[] getTemplate()
    {
        return m_Template;
    }

    /**
     * Set the user template.
     */
    public void setTemplate( byte[] value)
    {
        m_Template = value;
    }

    /**
     * Get the user unique identifier.
     */
    public byte[] getUniqueID()
    {
        return m_Key;
    }
    
    public FtrIdentifyRecord getFtrIdentifyRecord()
    {
        FtrIdentifyRecord r = new FtrIdentifyRecord();
        r.m_KeyValue = m_Key;
        r.m_Template = m_Template;
        
        return r;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String identifier) {
		this.uuid = identifier;
	}

	public String getAge() {
		return age;
	}

	public void setAge(String age) {
		this.age = age;
	}
	
        
   public String getDay()
{
   return day;
}

public void setDay( String day )
{
   this.day = day;
}

public String getMonth()
{
   return month;
}

public void setMonth( String month )
{
   this.month = month;
}

public String getYear()
{
   return year;
}

public void setYear( String year )
{
   this.year = year;
}

public String getAmrsid()
{
   return amrsid;
}

public void setAmrsid( String amrsid )
{
   this.amrsid = amrsid;
}

public String getNextvisitDate()
{
   return nextvisitDate;
}

public void setNextvisitDate( String nextvisitDate )
{
   this.nextvisitDate = nextvisitDate;
}

public String getPatientId()
{
   return patientId;
}

public void setPatientId( String patientId )
{
   this.patientId = patientId;
}

public String getPhone()
{
   return phone;
}

public void setPhone( String phone )
{
   this.phone = phone;
}

public String getFinger()
{
   return finger;
}

public void setFinger( String finger )
{
   this.finger = finger;
}

public String getFingername()
{
   return fingername;
}

public void setFingername( String fingername )
{
   this.fingername = fingername;
}

public String getPhone2()
{
   return phone2;
}

public void setPhone2( String phone2 )
{
   this.phone2 = phone2;
}

public String getDuration()
{
   return duration;
}

public void setDuration( String duration )
{
   this.duration = duration;
}

public String getLocation()
{
   return location;
}

public void setLocation( String location )
{
   this.location = location;
}

public String getReceive()
{
   return receive;
}

public void setReceive( String receive )
{
   this.receive = receive;
}

public String getCondition()
{
   return condition;
}

public void setCondition( String condition )
{
   this.condition = condition;
}
 }

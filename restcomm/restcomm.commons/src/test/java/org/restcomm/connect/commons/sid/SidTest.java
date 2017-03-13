package org.restcomm.connect.commons.sid;

import static org.junit.Assert.*;

import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;

public class SidTest {

	/**
	 * testSidConversion String to Sid conversation
	 */
	@Test
	public void testSidConversion() {
		String sidString = "AC6dcfdfd531e44ae4ac30e8f97e071ab2";
		try{
			Sid sid = new Sid(sidString);
			assertTrue(sid != null);
		}catch(Exception e){
			fail("invalid validation");
		}
	}

	/**
	 * testNewCallSid: String to Sid conversation
	 */
	@Test
	public void testNewCallSid() {
		String sidString = "CA6dcfdfd531e44ae4ac30e8f97e071ab2-ID6dcfdfd531e44ae4ac30e8f97e071122";
		try{
			Sid sid = new Sid(sidString);
			assertTrue(sid != null);
		}catch(Exception e){
			fail("invalid validation");
		}
	}

	/**
	 * testOldCallSid: it should be supported
	 */
	@Test
	public void testOldCallSid() {
		String sidString = "CA6dcfdfd531e44ae4ac30e8f97e071ab2";
		try{
			Sid sid = new Sid(sidString);
			assertTrue(sid != null);
		}catch(Exception e){
			fail("invalid validation");
		}
	}

}
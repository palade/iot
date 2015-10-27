package org.openiot.gsn.wrappers.evaluation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.log4j.Logger;
import org.openiot.gsn.beans.AddressBean;
import org.openiot.gsn.beans.DataField;
import org.openiot.gsn.beans.StreamElement;
import org.openiot.gsn.wrappers.AbstractWrapper;

public class BusServiceWrapper extends AbstractWrapper {
   
    private int DEFAULT_RATE = 2000;

    private static int threadCounter = 0;

    private final transient Logger logger = Logger.getLogger(BusServiceWrapper.class);

    private String urlPath;

    private URL url;

    private AddressBean addressBean;

    private String inputRate;

    private int rate;
   
   private transient final DataField [] outputStructure = new  DataField [] { 
       new DataField( "stopid" , "integer" , "Stop id" ),
       new DataField( "routeid" , "integer" , "Route id" ),
       new DataField( "available" , "integer" , "If the bus is available" )
   };
   
  
   /**
    * From XML file it needs the followings :
    * <ul>
    * <li>url</li> The full url for retriving the binary data.
    * <li>rate</li> The interval in msec for updating/asking for new information.
    * <li>mime</li> Type of the binary data.
    * </ul>
    */
    public boolean initialize() {
        this.addressBean = getActiveAddressBean();
        urlPath = this.addressBean.getPredicateValue("url");
        try {
            url = new URL(urlPath);
        } catch (MalformedURLException e) {
            logger.error("Loading the http wrapper failed : " + e.getMessage(), e);
            return false;
        }
        inputRate = this.addressBean.getPredicateValue("rate");
        if (inputRate == null || inputRate.trim().length() == 0) {
            rate = DEFAULT_RATE;
        } else {
            rate = Integer.parseInt(inputRate);
        }
        setName("HttpReceiver-Thread" + (++threadCounter));
        if (logger.isDebugEnabled()) {
            logger.debug("BusService is now running @" + rate + " Rate.");
        }
        return true;
    }
   
    public void run() {
        while (isActive()) {
            try {
                Thread.sleep(rate);

                StringBuilder response = new StringBuilder();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(url.openConnection().getInputStream()));
                String temp = "";

                while (null != (temp = br.readLine())) {
                    response.append(temp);
                }
                br.close();
                
                Serializable[] output = new Serializable[this.getOutputFormat().length];
                String[] values = response.toString().split(",");

                for (int i = 0; i < outputStructure.length; i++) {

                    String type = outputStructure[i].getType();

                    switch (type) {
                        case "integer":
                            output[i] = Integer.parseInt(values[i].trim());
                            break;
                        case "double":
                            output[i] = Double.parseDouble(values[i].trim());
                            break;
                    }
                }
                StreamElement se = new StreamElement(outputStructure, output);
                this.postStreamElement(se);
                
            } catch (InterruptedException | IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
    
    public String getWrapperName() {
        return "Bus Service Receiver";
    }

    public void dispose() {
        threadCounter--;
    }

    public DataField[] getOutputFormat() {
        return outputStructure;
    }
}
/**
 *  neuroConstruct
 *  Software for developing large scale 3D networks of biologically realistic neurons
 * 
 *  Copyright (c) 2009 Padraig Gleeson
 *  UCL Department of Neuroscience, Physiology and Pharmacology
 *
 *  Development of this software was made possible with funding from the
 *  Medical Research Council and the Wellcome Trust
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package ucl.physiol.neuroconstruct.hpc.mpi;

import java.io.File;
import java.util.*;

import ucl.physiol.neuroconstruct.utils.ClassLogger;


/**
 * Support for interacting with MPI platform
 *
 * @author Padraig Gleeson
 *  
 */


public class MpiSettings
{
    ClassLogger logger = new ClassLogger("MpiSettings");
    
    public static final String MPICH_V1 = "MPICH v1.*";
    public static final String MPICH_V2 = "MPICH v2.*";
    public static final String OPENMPI_V2 = "OPENMPI v2.*";

    private String version = MPICH_V1;

    public static int prefConfig = 0;
    
    
    public static final String LOCAL_SERIAL = "Local machine, serial mode";
    public static final String LOCAL_2PROC = "Local machine (2p)";
    public static final String LOCAL_3PROC = "Local machine (3p)";
    public static final String LOCAL_4PROC = "Local machine (4p)";

    public static final String OTHER_4PROC = "Other machine (4p)";

    public static final String CLUSTER_1PROC = "Cluster (1p)";
    public static final String CLUSTER_4PROC = "Cluster (1 x 4p)";
    public static final String CLUSTER_8PROC = "Cluster (1 x 8p)";
    public static final String CLUSTER_12PROC = "Cluster (1 x 12p)";
    public static final String CLUSTER_16PROC = "Cluster (1 x 16p)";
    public static final String CLUSTER_24PROC = "Cluster (6 x 4p)";
    public static final String CLUSTER_48PROC = "Cluster (12 x 4p)";
    public static final String CLUSTER_80PROC = "Cluster (20 x 4p)";
    
    public static final String MACHINE_FILE = "machinesToUse";
    
    public static final String LOCALHOST = "localhost";
    
    
    


    private ArrayList<MpiConfiguration> configurations = new ArrayList<MpiConfiguration>();

    public MpiSettings()
    {
        //String local8Config = "Local machine (8p)";
        //String local32Config = "Local machine (32p)";
        //String local128Config = "Local machine (128p)";
        String multiConfig = "TestConf";
        //String testConfig22 = "TestConfMore";

        RemoteLogin r900Login = new RemoteLogin("smp-test.rc.ucl.ac.uk", "ucgbpgl", "/home/ucgbpgl/nCsims", "/home/ucgbpgl/nrn62/x86_64/bin/nrniv");


        if (getMpiConfiguration(LOCAL_SERIAL)==null)
        {
            MpiConfiguration def = new MpiConfiguration(LOCAL_SERIAL);
            def.getHostList().add(new MpiHost(LOCALHOST, 1, 1));
            configurations.add(def);
        }


        if (getMpiConfiguration(LOCAL_2PROC)==null)
        {
            MpiConfiguration p = new MpiConfiguration(LOCAL_2PROC);
            p.getHostList().add(new MpiHost(LOCALHOST,2, 1));
            configurations.add(p);
        }
        if (getMpiConfiguration(LOCAL_4PROC)==null)
        {
            MpiConfiguration p = new MpiConfiguration(LOCAL_4PROC);
            p.getHostList().add(new MpiHost(LOCALHOST,4, 1));
            configurations.add(p);
        }
        if (getMpiConfiguration(LOCAL_3PROC)==null)
        {
            MpiConfiguration p = new MpiConfiguration(LOCAL_3PROC);
            p.getHostList().add(new MpiHost(LOCALHOST,3, 1));
            configurations.add(p);
        }
        if (getMpiConfiguration(OTHER_4PROC)==null)
        {
            MpiConfiguration p = new MpiConfiguration(OTHER_4PROC);
            p.getHostList().add(new MpiHost(LOCALHOST,4, 1));
            
            //p.setRemoteLogin(new RemoteLogin("192.168.15.70", "/tmp"));


            configurations.add(p);
        } /*
        if (getMpiConfiguration(local8Config)==null)
        {
            MpiConfiguration p = new MpiConfiguration(local8Config);
            p.getHostList().add(new MpiHost("localhost",8, 1));
            configurations.add(p);
        }
       
        if (getMpiConfiguration(local32Config)==null)
        {
            MpiConfiguration p = new MpiConfiguration(local32Config);
            p.getHostList().add(new MpiHost("localhost",32, 1));
            configurations.add(p);
        }
        if (getMpiConfiguration(local128Config)==null)
        {
            MpiConfiguration p = new MpiConfiguration(local128Config);
            p.getHostList().add(new MpiHost("localhost",128, 1));
            configurations.add(p);
        }
*/
        if (getMpiConfiguration(multiConfig)==null)
        {
            MpiConfiguration p = new MpiConfiguration(multiConfig);
            //p.getHostList().add(new MpiHost("padraigneuro", 1, 1));
            p.getHostList().add(new MpiHost("eriugena",4, 1));
            p.getHostList().add(new MpiHost("bernal", 4, 1));
            configurations.add(p);
        }

        if (getMpiConfiguration(CLUSTER_1PROC)==null)
        {
            MpiConfiguration p = new MpiConfiguration(CLUSTER_1PROC);

            p.getHostList().add(new MpiHost("localhost",1, 1));
            p.setRemoteLogin(r900Login);
            configurations.add(p);
        }

        if (getMpiConfiguration(CLUSTER_4PROC)==null)
        {
            MpiConfiguration p = new MpiConfiguration(CLUSTER_4PROC);

            p.getHostList().add(new MpiHost("localhost",4, 1));
            p.setRemoteLogin(r900Login);
            configurations.add(p);
        }

        if (getMpiConfiguration(CLUSTER_8PROC)==null)
        {
            MpiConfiguration p = new MpiConfiguration(CLUSTER_8PROC);
            p.getHostList().add(new MpiHost("localhost",8, 1));
            p.setRemoteLogin(r900Login);
            configurations.add(p);
        }

        if (getMpiConfiguration(CLUSTER_12PROC)==null)
        {
            MpiConfiguration p = new MpiConfiguration(CLUSTER_12PROC);
            p.getHostList().add(new MpiHost("localhost",12, 1));
            p.setRemoteLogin(r900Login);
            configurations.add(p);
        }

        if (getMpiConfiguration(CLUSTER_16PROC)==null)
        {
            MpiConfiguration p = new MpiConfiguration(CLUSTER_16PROC);
            p.getHostList().add(new MpiHost("localhost",16, 1));
            p.setRemoteLogin(r900Login);
            configurations.add(p);
        }
        
        if (getMpiConfiguration(CLUSTER_24PROC)==null)
        {
            MpiConfiguration p = new MpiConfiguration(CLUSTER_24PROC);
            for(int i=0;i<6;i++)
                p.getHostList().add(new MpiHost("node"+i,4, 1));
            configurations.add(p);
        }
        
        if (getMpiConfiguration(CLUSTER_48PROC)==null)
        {
            MpiConfiguration p = new MpiConfiguration(CLUSTER_48PROC);
            for(int i=0;i<12;i++)
                p.getHostList().add(new MpiHost("node"+i,4, 1));
            configurations.add(p);
        }
        
        
        if (getMpiConfiguration(CLUSTER_80PROC)==null)
        {
            MpiConfiguration p = new MpiConfiguration(CLUSTER_80PROC);
            for(int i=0;i<20;i++)
                p.getHostList().add(new MpiHost("node"+i,4, 1));
            configurations.add(p);
        }
        
        


    }


    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getVersion()
    {
        File mpichV1flag = new File("MPICH1");
        File mpichV2flag = new File("MPICH2");
        File openmpiV2flag = new File("MPI2");
        if (mpichV1flag.exists()) return MPICH_V1;
        if (mpichV2flag.exists()) return MPICH_V2;
        if (openmpiV2flag.exists()) return OPENMPI_V2;
        
        return this.version;
    }

    public ArrayList<MpiConfiguration> getMpiConfigurations()
    {
        return this.configurations;
    }

    public void setMpiConfigurations(ArrayList<MpiConfiguration> confs)
    {
        this.configurations = confs;
    }

    public MpiConfiguration getMpiConfiguration(String name)
    {
        for (MpiConfiguration config: configurations)
        {
            if (config.getName().equals(name)) return config;
        }

        return null;
    }

    public static void main(String[] args)
    {
        new MpiSettings();
    }

}





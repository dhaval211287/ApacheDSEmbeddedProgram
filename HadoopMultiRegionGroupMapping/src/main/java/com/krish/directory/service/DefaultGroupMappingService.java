package com.krish.directory.service;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.krish.security.hadoop.impl.GroupsMappingBuilder;
import com.krish.security.hadoop.impl.MultiRegionGroups;

public class DefaultGroupMappingService {

  private GroupsMappingBuilder groupServiceBuilder = new GroupsMappingBuilder();
  private static final Logger LOG = LoggerFactory.getLogger(DefaultGroupMappingService.class);
  private EadSchemaService schemaService;

  public void setEadSchemaService(EadSchemaService schemaService) {
    this.schemaService = schemaService;
  }

  public void buildGroupMapping(Path groupMappingXml) throws Exception {
    Configuration conf = new Configuration();
    conf.addResource(groupMappingXml);
    groupServiceBuilder.buildCompositeGroupMappingProviders(conf);
  }

  public void doSchemaUpdate() {
    List<MultiRegionGroups> groupProviders = groupServiceBuilder.getProvidersList();
    for (MultiRegionGroups groupProvider : groupProviders) {
      List<String> groups = groupProvider.getGroups();
      GroupMappingServiceProvider groupMappingProvider = groupProvider.getGroupServiceProvider();
      for (String group : groups) {
        try {
          List<String> users = groupMappingProvider.getUsers(group);
          LOG.info("The users for the group " + group + " are " + users);
          System.out.println("The users for the group " + group + " are " + users);

          // Call the update Schema here
          doSchemaUpdateIfNecessary(group, users);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  private void doSchemaUpdateIfNecessary(String group, List<String> users) {
    // Check if the group Exist
    try {
      if(!schemaService.checkIfGroupExist(group) && users.size()>0){
        schemaService.createGroup(group);
      }
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    
    for (String user : users) {
      try {
        boolean ifUserExist = schemaService.checkIfUserExist(user);
        if (!ifUserExist) {
          schemaService.createUser(user, "password");
        }
        if (!schemaService.checkIfUserMemberOfGroup(user, group)) {
          schemaService.addUserToGroup(user, group);
        }

      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

  }

  public static void main(String[] args) throws Exception {
    DefaultGroupMappingService groupMappingService = new DefaultGroupMappingService();
    groupMappingService.buildGroupMapping(null);
    groupMappingService.doSchemaUpdate();
  }
}

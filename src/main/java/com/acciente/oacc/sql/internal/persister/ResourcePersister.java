/*
 * Copyright 2009-2015, Acciente LLC
 *
 * Acciente LLC licenses this file to you under the
 * Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.acciente.oacc.sql.internal.persister;

import com.acciente.oacc.Resource;
import com.acciente.oacc.sql.internal.persister.id.DomainId;
import com.acciente.oacc.sql.internal.persister.id.Id;
import com.acciente.oacc.sql.internal.persister.id.ResourceClassId;
import com.acciente.oacc.sql.internal.persister.id.ResourceId;

import java.sql.SQLException;

public class ResourcePersister extends Persister {
   private final SQLStrings sqlStrings;

   public ResourcePersister(SQLStrings sqlStrings) {
      this.sqlStrings = sqlStrings;
   }

   public void verifyResourceExists(SQLConnection connection,
                                    Resource resource) {
      SQLStatement statement = null;

      try {
         SQLResult resultSet;

         statement = connection.prepareStatement(sqlStrings.SQL_findInResource_ResourceId_BY_ResourceID);
         statement.setResourceId(1, resource);
         resultSet = statement.executeQuery();

         // complain if we do not find the resource
         if (!resultSet.next()) {
            throw new IllegalArgumentException("Resource " + resource + " not found!");
         }

         // complain if we found more than one resource!
         // (assuming the PK constraint is being enforced by the DB, currently do not see how this can happen)
         if (resultSet.next()) {
            throw new IllegalStateException("Resource reference " + resource + " maps to more than one resource!");
         }
      }
      catch (SQLException e) {
         throw new RuntimeException(e);
      }
      finally {
         closeStatement(statement);
      }
   }

   public void createResource(SQLConnection connection,
                              Id<ResourceId> newResourceId,
                              Id<ResourceClassId> resourceClassId,
                              Id<DomainId> resourceDomainId) {
      SQLStatement statement = null;

      try {
         statement = connection.prepareStatement(sqlStrings.SQL_createInResource_WITH_ResourceID_ResourceClassID_DomainID);
         statement.setResourceId(1, newResourceId);
         statement.setResourceClassId(2, resourceClassId);
         statement.setResourceDomainId(3, resourceDomainId);

         assertOneRowInserted(statement.executeUpdate());
      }
      catch (SQLException e) {
         throw new RuntimeException(e);
      }
      finally {
         closeStatement(statement);
      }
   }

   public void deleteResource(SQLConnection connection,
                              Resource resource) {
      SQLStatement statement = null;

      try {
         statement = connection.prepareStatement(sqlStrings.SQL_removeInResource_BY_ResourceID);
         statement.setResourceId(1, resource);

         assertOneRowUpdated(statement.executeUpdate());
      }
      catch (SQLException e) {
         throw new RuntimeException(e);
      }
      finally {
         closeStatement(statement);
      }
   }

   public Id<DomainId> getDomainIdByResource(SQLConnection connection,
                                             Resource resource) {
      SQLStatement statement = null;

      try {
         Id<DomainId> domainId = null;

         statement = connection.prepareStatement(sqlStrings.SQL_findInResource_DomainID_BY_ResourceID);
         statement.setResourceId(1, resource);
         SQLResult resultSet = statement.executeQuery();

         if (resultSet.next()) {
            domainId = resultSet.getResourceDomainId("DomainId");
         }

         if (domainId == null) {
            throw new IllegalArgumentException("Could not determine domain for resource: " + resource);
         }

         return domainId;
      }
      catch (SQLException e) {
         throw new RuntimeException(e);
      }
      finally {
         closeStatement(statement);
      }
   }

   public Id<ResourceId> getNextResourceId(SQLConnection connection) {
      SQLStatement statement = null;
      Id<ResourceId> newResourceId = null;

      try {
         SQLResult resultSet;

         statement = connection.prepareStatement(sqlStrings.SQL_nextResourceID);
         resultSet = statement.executeQuery();

         if (resultSet.next()) {
            newResourceId = resultSet.getNextResourceId(1);
         }

         resultSet.close();

         return newResourceId;
      }
      catch (SQLException e) {
         throw new RuntimeException(e);
      }
      finally {
         closeStatement(statement);
      }
   }

   public int getResourceCountForDomain(SQLConnection connection,
                                        Id<DomainId> resourceDomainId) {
      SQLStatement statement = null;

      try {
         SQLResult resultSet;

         statement = connection.prepareStatement(sqlStrings.SQL_findInResource_COUNTResourceID_BY_DomainID);
         statement.setResourceDomainId(1, resourceDomainId);
         resultSet = statement.executeQuery();

         if (!resultSet.next()) {
            throw new IllegalArgumentException("Could not read resource count for domain: " + resourceDomainId);
         }

         final int count = resultSet.getInteger("COUNTResourceID");

         resultSet.close();

         return count;
      }
      catch (SQLException e) {
         throw new RuntimeException(e);
      }
      finally {
         closeStatement(statement);
      }
   }
}

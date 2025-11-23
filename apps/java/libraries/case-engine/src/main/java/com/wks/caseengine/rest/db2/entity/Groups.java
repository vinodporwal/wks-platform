package com.wks.caseengine.rest.db2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.GenericGenerator;

@Entity
public class Groups {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "Group_PK_ID")
    private String groupPkId;

    @Column(name = "GroupId", length = 100)
    private String groupId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "group_users",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<Users> users = new HashSet<>();

   public String getGroupId() {
    return groupId;
   }

   public Set<Users> getUsers() {
    return users;
   }
}

package com.coursistant.lms.module.sales.entity;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalesRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String phone = null;
    private String additionalInfo = null;

    private String receivedFrom;
    private String messageType = "Sales Enquiry";
    private String entity = null;
    private String company = null;
    private Integer companySize = null;
    private String role = null;
    private String department = null;


    
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    private void setEntity(String entity)
    {
        this.entity = entity;
    }

    private String getEntity()
    {
        return this.entity;
    }

    private void setCompany(String company)
    {
        this.company = company;
    }

    private String getCompany()
    {
        return this.company;
    }

    private void setRole(String role)
    {
        this.role = role;
    }

    private String getRole()
    {
        return this.role;
    }

    private void setCompanySize()
    {
        this.companySize = companySize;
    }

    private Integer getCompanySize()
    {
        return this.companySize;
    }

    private void setDepartment(String department)
    {
        this.department = department;
    }

    private String getDepartment()
    {
        return this.department;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }
    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public String getReceivedFrom() {
        return receivedFrom;
    }

    public void setReceivedFrom(String receivedFrom) {
        this.receivedFrom = receivedFrom;
    }

    public String getMessageType() {
        return messageType;
    }
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    @Override
    public String toString() {

        try{
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        }

        catch(Exception e)
        {
            return super.toString();
        }
     }

}

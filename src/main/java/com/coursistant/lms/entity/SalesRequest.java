package com.coursistant.lms.entity;

public class SalesRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String phone = null;
    private String entity = null;
    private String company = null;
    private Integer companySize = null;
    private String role = null;
    private String department = null;
    private String additionalInfo = null;
    private String receivedFrom;
    private String messageType = "Sales Enquiry";

    
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
        StringBuilder sb = new StringBuilder();
        sb.append("firstName: ").append(firstName);
        sb.append("\n");
        sb.append(", lastName: ").append(lastName);
        sb.append("\n");
        sb.append(", entity: ").append(entity);
        sb.append("\n");
        sb.append(", email: ").append(email);
        sb.append("\n");
        sb.append(", phone: ").append(phone);
        sb.append("\n");
        sb.append(", company: ").append(company);
        sb.append("\n");
        sb.append(", companySize: ").append(companySize);
        sb.append("\n");
        sb.append(", role: ").append(role);
        sb.append("\n");
        sb.append(", department: ").append(department);
        sb.append("\n");
        sb.append(", additionalInfo: ").append(additionalInfo);
        sb.append("\n");
        sb.append(", receivedFrom: ").append(receivedFrom);
        sb.append("\n");
        sb.append(", messageType: ").append(messageType);
        sb.append("\n");
        return sb.toString();
    }

}

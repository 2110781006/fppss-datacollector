/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc.model;

import java.util.Objects;

/**
 * ProviderAccountObject
 */
public class ProviderAccountObject
{
  private String providerName;

  private String providerFullName;

  private Integer providerType;

  private Integer providerId;

  private Integer providerAccountId;

  private String providerAccountUsername;

  private String providerAccountPassword;

  public ProviderAccountObject providerName(String providerName) {
    this.providerName = providerName;
    return this;
  }

  /**
   * provider short name
   * @return providerName
  */


  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public ProviderAccountObject providerFullName(String providerFullName) {
    this.providerFullName = providerFullName;
    return this;
  }

  /**
   * provider full name
   * @return providerFullName
  */


  public String getProviderFullName() {
    return providerFullName;
  }

  public void setProviderFullName(String providerFullName) {
    this.providerFullName = providerFullName;
  }

  public ProviderAccountObject providerType(Integer providerType) {
    this.providerType = providerType;
    return this;
  }

  /**
   * provider type 0=energy provider, 1=inverter
   * @return providerType
  */


  public Integer getProviderType() {
    return providerType;
  }

  public void setProviderType(Integer providerType) {
    this.providerType = providerType;
  }

  public ProviderAccountObject providerId(Integer providerId) {
    this.providerId = providerId;
    return this;
  }

  /**
   * provider id
   * @return providerId
  */


  public Integer getProviderId() {
    return providerId;
  }

  public void setProviderId(Integer providerId) {
    this.providerId = providerId;
  }

  public ProviderAccountObject providerAccountId(Integer providerAccountId) {
    this.providerAccountId = providerAccountId;
    return this;
  }

  /**
   * id of provider account
   * @return providerAccountId
  */


  public Integer getProviderAccountId() {
    return providerAccountId;
  }

  public void setProviderAccountId(Integer providerAccountId) {
    this.providerAccountId = providerAccountId;
  }

  public ProviderAccountObject providerAccountUsername(String providerAccountUsername) {
    this.providerAccountUsername = providerAccountUsername;
    return this;
  }

  /**
   * provider account user name
   * @return providerAccountUsername
  */


  public String getProviderAccountUsername() {
    return providerAccountUsername;
  }

  public void setProviderAccountUsername(String providerAccountUsername) {
    this.providerAccountUsername = providerAccountUsername;
  }

  public ProviderAccountObject providerAccountPassword(String providerAccountPassword) {
    this.providerAccountPassword = providerAccountPassword;
    return this;
  }

  /**
   * provider account user password
   * @return providerAccountPassword
  */


  public String getProviderAccountPassword() {
    return providerAccountPassword;
  }

  public void setProviderAccountPassword(String providerAccountPassword) {
    this.providerAccountPassword = providerAccountPassword;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProviderAccountObject providerAccountObject = (ProviderAccountObject) o;
    return Objects.equals(this.providerName, providerAccountObject.providerName) &&
        Objects.equals(this.providerFullName, providerAccountObject.providerFullName) &&
        Objects.equals(this.providerType, providerAccountObject.providerType) &&
        Objects.equals(this.providerId, providerAccountObject.providerId) &&
        Objects.equals(this.providerAccountId, providerAccountObject.providerAccountId) &&
        Objects.equals(this.providerAccountUsername, providerAccountObject.providerAccountUsername) &&
        Objects.equals(this.providerAccountPassword, providerAccountObject.providerAccountPassword);
  }

  @Override
  public int hashCode() {
    return Objects.hash(providerName, providerFullName, providerType, providerId, providerAccountId, providerAccountUsername, providerAccountPassword);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProviderAccountObject {\n");
    
    sb.append("    providerName: ").append(toIndentedString(providerName)).append("\n");
    sb.append("    providerFullName: ").append(toIndentedString(providerFullName)).append("\n");
    sb.append("    providerType: ").append(toIndentedString(providerType)).append("\n");
    sb.append("    providerId: ").append(toIndentedString(providerId)).append("\n");
    sb.append("    providerAccountId: ").append(toIndentedString(providerAccountId)).append("\n");
    sb.append("    providerAccountUsername: ").append(toIndentedString(providerAccountUsername)).append("\n");
    sb.append("    providerAccountPassword: ").append(toIndentedString(providerAccountPassword)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}


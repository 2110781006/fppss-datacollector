package org.fppssdc.model;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Objects;
import java.math.BigDecimal;
import java.time.OffsetDateTime;


/**
 * TimeValueObject
 */
public class TimeValueObject   {

  private OffsetDateTime timestamp;
  private String meeterId;
  private String datapointname;
  private Integer providerAccountId;
  private BigDecimal value;
  private BigDecimal counterValue;

  public TimeValueObject(OffsetDateTime timestamp, String meeterId, String datapointname, Integer providerAccountId, BigDecimal value, BigDecimal counterValue, Boolean feedin)
  {
    this.timestamp = timestamp;
    this.meeterId = meeterId;
    this.datapointname = datapointname;
    this.providerAccountId = providerAccountId;
    this.value = value;
    this.counterValue = counterValue;
    this.feedin = feedin;
  }

  private Boolean feedin;
  public TimeValueObject timestamp(OffsetDateTime timestamp) {
    this.timestamp = timestamp;
    return this;
  }


  public OffsetDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(OffsetDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public TimeValueObject datapointname(String datapointname) {
    this.datapointname = datapointname;
    return this;
  }

  /**
   * name of the datapoint
   * @return datapointname
  */

  public String getDatapointname() {
    return datapointname;
  }

  public void setDatapointname(String datapointname) {
    this.datapointname = datapointname;
  }

  public TimeValueObject providerAccountId(Integer providerAccountId) {
    this.providerAccountId = providerAccountId;
    return this;
  }

  /**
   * provider account id
   * @return providerAccountId
  */

  public Integer getProviderAccountId() {
    return providerAccountId;
  }

  public void setProviderAccountId(Integer providerAccountId) {
    this.providerAccountId = providerAccountId;
  }

  public TimeValueObject value(BigDecimal value) {
    this.value = value;
    return this;
  }

  /**
   * diff value since last value
   * @return value
  */

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public TimeValueObject counterValue(BigDecimal counterValue) {
    this.counterValue = counterValue;
    return this;
  }

  /**
   * counter value of the smart meeter
   * @return counterValue
  */

  public BigDecimal getCounterValue() {
    return counterValue;
  }

  public void setCounterValue(BigDecimal counterValue) {
    this.counterValue = counterValue;
  }

  public TimeValueObject feedin(Boolean feedin) {
    this.feedin = feedin;
    return this;
  }

  /**
   * feedin value or consumption value
   * @return feedin
  */


  public Boolean getFeedin() {
    return feedin;
  }

  public void setFeedin(Boolean feedin) {
    this.feedin = feedin;
  }

  public String getMeeterId()
  {
    return meeterId;
  }

  public void setMeeterId(String meeterId)
  {
    this.meeterId = meeterId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TimeValueObject timeValueObject = (TimeValueObject) o;
    return Objects.equals(this.timestamp, timeValueObject.timestamp) &&
        Objects.equals(this.meeterId, timeValueObject.meeterId) &&
        Objects.equals(this.datapointname, timeValueObject.datapointname) &&
        Objects.equals(this.providerAccountId, timeValueObject.providerAccountId) &&
        Objects.equals(this.value, timeValueObject.value) &&
        Objects.equals(this.counterValue, timeValueObject.counterValue) &&
        Objects.equals(this.feedin, timeValueObject.feedin);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, datapointname, providerAccountId, value, counterValue, feedin);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TimeValueObject {\n");
    
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    datapointname: ").append(toIndentedString(datapointname)).append("\n");
    sb.append("    providerAccountId: ").append(toIndentedString(providerAccountId)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    counterValue: ").append(toIndentedString(counterValue)).append("\n");
    sb.append("    feedin: ").append(toIndentedString(feedin)).append("\n");
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

  public String toJson()
  {
    Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(OffsetDateTime.class, new JsonSerializer<>()
            {
              @Override
              public JsonElement serialize(Object o, Type type, JsonSerializationContext jsonSerializationContext)
              {
                OffsetDateTime oo = (OffsetDateTime) o;

                return new JsonPrimitive(oo.toString());
              }
            })
            .create();

    return gson.toJson(this);
  }

  public static String toJson(ArrayList<TimeValueObject> timeValueObjects)
  {
    Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(OffsetDateTime.class, new JsonSerializer<>()
            {
              @Override
              public JsonElement serialize(Object o, Type type, JsonSerializationContext jsonSerializationContext)
              {
                OffsetDateTime oo = (OffsetDateTime) o;

                return new JsonPrimitive(oo.toString());
              }
            })
            .create();

    return gson.toJson(timeValueObjects);
  }

  //resolution of timerange
  public enum Resolution
  {
    spontan,
    hour,
    day,
    month,
    year
  }
}


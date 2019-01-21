package com.warn.dao;

import com.warn.entity.Bed;

public interface BedDao {
    Bed getBedBySPId(String sensorPointId);

    Bed getBedByOid(Integer oid);


}

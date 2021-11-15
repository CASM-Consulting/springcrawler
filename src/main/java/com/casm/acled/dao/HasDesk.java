package com.casm.acled.dao;

import java.util.List;

public interface HasDesk<T> {
    List<T> byDesk(Integer deskId);
}

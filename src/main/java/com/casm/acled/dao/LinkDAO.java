package com.casm.acled.dao;

import com.casm.acled.entities.VersionedEntity;

import java.util.List;
import java.util.Optional;

public interface LinkDAO<L1 extends VersionedEntity<L1>, L2 extends VersionedEntity<L2>, V extends VersionedEntity<V>> extends VersionedEntityDAO<V> {

    default V link(L1 entity1, L2 entity2) {
        return link(entity1.id(), entity2.id());
    }

    V link(int entity1Id, int entity2Id);

    default void unlink(L1 entity1, L2 entity2) {
        unlink(entity1.id(), entity2.id());
    }

    void unlink(int entity1Id, int entity2Id);

    default void unlink1(L1 entity1) {
        unlink1(entity1.id());
    }

    void unlink1(int entity1Id);

    default void unlink2(L2 entity2) {
        unlink2(entity2.id());
    }
    void unlink2(int entity2Id);

    List<V> getBy2(L2 l2);
    List<V> getBy1(L1 l1);

    Optional<V> get(int id1, int id2);
    default Optional<V> get(L1 l1, L2 l2)  {
        return get(l1.id(), l2.id());
    }

    void clear();

    String table();

}

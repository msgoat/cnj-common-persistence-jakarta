package group.msg.at.cloud.common.persistence.jpa.repository;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 * Generic {@code Repository} providing a thin abstraction layer for a JPA
 * {@link EntityManager}, in order to have a common implementation of common JPA
 * access methods.
 * <p>
 * Concrete stateless session bean subclasses have to implement
 * {@link #getEntityManager()} in order to provide an entity manager for a
 * specific persistence unit. Due to this delegation,
 * {@code AbstractGenericRepositoryBean} is able to support multiple persistence
 * units in one JPA module.
 * </p>
 *
 * @author Michael Theis (michael.theis@msg.group)
 * @version 1.0
 * @since release 1.0.0
 */
public abstract class AbstractGenericRepository {

    protected final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * SessionContext to obtain the currently authenticated user.
     */
    @Resource
    SessionContext sessionContext;

    /**
     * Adds the given {@code Entity} to the datastore and updates the given entity
     * with the current state of the datastore.
     *
     * @param entity entity to be added to the datastore
     */
    public void addEntity(Object entity) {
        addEntity(entity, true);
    }

    /**
     * Adds the given {@code Entity} to the datastore and updates the given entity
     * with the current state of the datastore.
     *
     * @param entity  entity to be added to the datastore
     * @param refresh controls, if given entity should be refreshed with state modified
     *                by the datastore
     */
    public void addEntity(Object entity, boolean refresh) {
        getEntityManager().persist(entity);
        if (refresh) {
            getEntityManager().flush();
            getEntityManager().refresh(entity);
        }
    }

    /**
     * Returns an {@code Entity} of the given type having the given unique
     * identifier.
     *
     * @param entityType expected type of the entity
     * @param entityId   unique primary key of the entity
     * @return found entity, if an entity with the given unique identifier could be
     * found; ; otherwise {@code null}
     */
    public <T> T getEntityById(Class<T> entityType, Object entityId) {
        return getEntityManager().find(entityType, entityId);
    }

    /**
     * Liefert die Entit??t vom angebenen Typ mit dem angebenen eindeutigen
     * technischen Bezeichner zur??ck, wobei erwartet wird, dass eine Entit??t mit dem
     * angegeben Beeichner existiert.
     *
     * @param entityType erwartete Entit??tentyp
     * @param entityId   eindeutiger technischer Bezeichner
     * @return gefundene Entit??t, falls eine Entit??t mit dem angebenen Bezeichner
     * exisitert; niemals {@code null}
     * @throws NoSuchElementException - falls keine Entit??t mit dem angegebenen Bezeichner gefunden
     *                                werden kann.
     */
    public <T> T getRequiredEntityById(Class<T> entityType, Object entityId) {
        final T result = getEntityById(entityType, entityId);
        if (result == null) {
            throw new NoSuchElementException("Missing required entity of type [" + entityType.getName()
                    + "] with unique identifier [" + entityId + "]!");
        }
        return result;
    }

    /**
     * Aktualisiert den Zustand der Entit??t im Datastore mit der angegebenen
     * Entit??t.
     * <p>
     * Die angegebene Entit??t muss zuvor mit {{@link #addEntity(Object)} dem
     * Datastore hinzugef??gt worden sein.
     * </p>
     *
     * @param entity zu aktualisierende Entit??t
     */
    public void setEntity(Object entity) {
        getEntityManager().merge(entity);
    }

    /**
     * Entfernt die angegebene Entit??t aus dem Datastore.
     * <p>
     * Die angegebene Entit??t muss zuvor mit {{@link #addEntity(Object)} dem
     * Datastore hinzugef??gt worden sein.
     * </p>
     *
     * @param entity zu l??schende Entit??t
     */
    public void removeEntity(Object entity) {
        final Object mergedEntity = getEntityManager().merge(entity);
        getEntityManager().remove(mergedEntity);
    }

    /**
     * Entfernt die Entit??t mit dem angegebenen eindeutigen Bezeichner aus dem
     * Datastore.
     * <p>
     * Achtung: Beim L??schen ??ber ID ist zu beachten, dass optimistische Sperren
     * nicht gepr??ft werden k??nnen.
     * </p>
     *
     * @param entityType erwarteter Entit??tentyp
     * @param entityId   eindeutiger technischer Bezeichner der zu l??schende Entit??t
     */
    public <T> void removeEntityById(Class<T> entityType, Object entityId) {
        final T entity = getEntityManager().find(entityType, entityId);
        if (entity != null) {
            getEntityManager().remove(entity);
        }
    }

    /**
     * Sucht eine einzelne Entit??t des angegebenen Typs mit der angegebenen Named
     * Query und den angegebenen Query-Parametern und liefert das Ergebnis der Query
     * zur??ck.
     *
     * @param entityType      erwarteter Entit??tentyp
     * @param queryName       eindeutiger Name einer Named Query
     * @param queryParameters anzuwendende Query-Parameter (optional)
     * @return gefundene Entit??t, falls die Query ein Ergebnis geliefert hat; sonst
     * {@code null}.
     */
    public <T> T queryEntity(Class<T> entityType, String queryName, QueryParameters queryParameters) {
        final TypedQuery<T> query = getEntityManager().createNamedQuery(queryName, entityType);
        if (queryParameters != null) {
            queryParameters.applyParameters(query);
        }
        T result = null;
        try {
            result = query.getSingleResult();
        } catch (final NoResultException ex) {
            // Es ist OK wenn nichts gefunden wird
        }
        return result;
    }

    /**
     * Sucht eine einzelne Entit??t des angegebenen Typs mit der angegebenen Named
     * Query und den angegebenen Query-Parametern und liefert das Ergebnis der Query
     * zur??ck.
     * <p>
     * Im Gegensatz zu {@link #queryEntity(Class, String, QueryParameters)} wird
     * allerdings davon ausgegangen, dass die gew??nschte Entit??t existiert.
     * </p>
     *
     * @param entityType      erwarteter Entit??tentyp
     * @param queryName       eindeutiger Name einer Named Query
     * @param queryParameters anzuwendende Query-Parameter (optional)
     * @return gefundene Entit??t, niemals {@code null}.
     * @throws NoSuchElementException - falls keine Entit??t gefunden werden kann
     */
    public <T> T queryRequiredEntity(Class<T> entityType, String queryName, QueryParameters queryParameters) {
        final T result = queryEntity(entityType, queryName, queryParameters);
        if (result == null) {
            throw new NoSuchElementException(
                    "Expected query [" + queryName + "] with query parameters [" + queryParameters
                            + "] to find exactly one entity of type [" + entityType + "] but actually found none!");
        }
        return result;
    }

    /**
     * Sucht eine Menge von Entit??t des angegebenen Typs mit der angegebenen Named
     * Query und den angegebenen Query-Parametern und liefert das Ergebnis der Query
     * zur??ck.
     *
     * @param entityType      erwarteter Entit??tentyp
     * @param queryName       eindeutiger Name einer Named Query
     * @param queryParameters anzuwendende Query-Parameter (optional)
     * @return Liste der gefundenen Entit??ten, niemals {@code null}
     */
    public <T> List<T> queryEntities(Class<T> entityType, String queryName, QueryParameters queryParameters) {
        final TypedQuery<T> query = getEntityManager().createNamedQuery(queryName, entityType);
        if (queryParameters != null) {
            queryParameters.applyParameters(query);
        }
        return query.getResultList();
    }

    /**
     * Ermittelt die Anzahl der Entit??ten des angegebenen Typs mit der angegebenen
     * Named Query und den angegebenen Query-Parametern und liefert das Ergebnis der
     * Query zur??ck.
     *
     * @param queryName       eindeutiger Name einer Named Query
     * @param queryParameters anzuwendende Query-Parameter (optional)
     * @return Anzahl der gefundenen Entit??ten, die zu den Query-Parametern passen
     */
    public long countEntities(String queryName, QueryParameters queryParameters) {
        final TypedQuery<Long> query = getEntityManager().createNamedQuery(queryName, Long.class);
        if (queryParameters != null) {
            queryParameters.applyParameters(query);
        }
        return query.getSingleResult();
    }

    /**
     * Sucht eine Menge von Entit??t des angegebenen Typs mit der angegebenen Named
     * Query und den angegebenen Query-Parametern und liefert das Ergebnis der Query
     * zur??ck.
     *
     * @param entityType      erwarteter Entit??tentyp
     * @param queryName       eindeutiger Name einer Named Query
     * @param queryParameters anzuwendende Query-Parameter (optional)
     * @param startIndex      Start-Index des ersten Treffers in der Ergebnismenge
     * @param pageSize        maximal Anzahl an Treffern pro Seite
     * @return Liste der gefundenen Entit??ten, niemals {@code null}
     */
    public <T> List<T> queryEntitiesWithPagination(Class<T> entityType, String queryName,
                                                   QueryParameters queryParameters, int startIndex, int pageSize) {
        final TypedQuery<T> query = getEntityManager().createNamedQuery(queryName, entityType);
        if (queryParameters != null) {
            queryParameters.applyParameters(query);
        }
        query.setFirstResult(startIndex);
        query.setMaxResults(pageSize);
        return query.getResultList();
    }

    /**
     * Template method to obtain an entity manager for a specific persistence unit
     * from concrete subclasses.
     */
    protected abstract EntityManager getEntityManager();
}

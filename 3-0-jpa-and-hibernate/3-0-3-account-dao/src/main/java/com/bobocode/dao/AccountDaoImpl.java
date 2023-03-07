package com.bobocode.dao;

import com.bobocode.exception.AccountDaoException;
import com.bobocode.model.Account;
import com.bobocode.util.ExerciseNotCompletedException;
import org.hibernate.Session;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class AccountDaoImpl implements AccountDao {
    private EntityManagerFactory emf;

    public AccountDaoImpl(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public void save(Account account) {
        performWithinPersistenceContext(em -> em.persist(account));
    }

    @Override
    public Account findById(Long id) {
        return performReturningWithinPersistenceContext(em->em.find(Account.class, id));
    }

    @Override
    public Account findByEmail(String email) {
        return performReturningWithinPersistenceContext((em)-> em.createQuery("SELECT a from Account a where a.email = :email", Account.class)
                .setParameter("email",email)
                .getSingleResult());
    }

    @Override
    public List<Account> findAll() {
        return performReturningWithinPersistenceContext((em)-> em.createQuery("SELECT a from Account a", Account.class)
                .getResultList());
    }

    @Override
    public void update(Account account) {
        performWithinPersistenceContext(em -> em.merge(account));
    }

    @Override
    public void remove(Account account) {
        performWithinPersistenceContext(em->{
            Account mergedAccount = em.merge(account);
            em.remove(account);
        });
    }

    private void performWithinPersistenceContext(Consumer<EntityManager> entityManagerConsumer){
        performReturningWithinPersistenceContext((em)->{
            entityManagerConsumer.accept(em);
            return null;
        });
    }

    private <T> T performReturningWithinPersistenceContext(Function<EntityManager, T> entityManagerFunction) {
        EntityManager em  = emf.createEntityManager();
        em.getTransaction().begin();
        T result;
        try {
            result = entityManagerFunction.apply(em);
            em.getTransaction().commit();
        }catch (Exception e){
            em.getTransaction().rollback();
            throw new AccountDaoException("Error performing dao operation. Transaction is rolled back!", e);
        }
        finally {
            em.close();
        }
        return result;
    }

}


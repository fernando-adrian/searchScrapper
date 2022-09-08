package com.cursojava.curso.repositories;

import com.cursojava.curso.entities.WebPage;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;

@Repository
public class SearchRepositoryImpl implements SearchRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    @Override
    public List<WebPage> search(String searchText) {
        String query = "FROM WebPage WHERE description like :searchText";
        return entityManager.createQuery(query)
                .setParameter("searchText", "%"+searchText+"%")
                .getResultList();
    }

    @Transactional
    @Override
    public void save(WebPage webPage) {
        entityManager.merge(webPage);
    }

    @Override
    public WebPage getByUrl(String url) {
        String query = "FROM WebPage WHERE url = :url";
        List<WebPage> list = entityManager.createQuery(query)
                .setParameter("url", url)
                .getResultList();
        return list.size() == 0 ? null : list.get(0);
    }

    @Override
    public List<WebPage> getLinksToIndex() {
        String query = "FROM WebPage WHERE title is null AND description is null";
        return entityManager.createQuery(query)
                .setMaxResults(100)
                .getResultList();
    }

    @Override
    public boolean exist(String url){
        return getByUrl(url) != null;
    }
}

package org.jboss.infinispan.demo;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaQuery;

import org.infinispan.Cache;
import org.jboss.infinispan.demo.model.Task;
import org.jboss.infinispan.demo.model.User;

/**
 * This class is used to query, insert or update Task object.
 * @author tqvarnst
 *
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class TaskService {

	@PersistenceContext
    EntityManager em;
	
	@Inject
	Cache<Long,User> cache;
	
	@Inject
	@DefaultUser
	User currentUser;
	
	@Inject
	UserService userService;
	
	Logger log = Logger.getLogger(this.getClass().getName());

	/**
	 * This methods should return all cache entries, currently contains mockup code. 
	 * @return
	 * 
	 * DONE: Replace implementation with Cache.values()
	 */
	public Collection<Task> findAll() {
		List<Task> tasks = cache.get(currentUser.getId()).getTasks();
		Collections.sort(tasks, new Comparator<Task>() {
			@Override
			public int compare(Task o1, Task o2) {
				return o1.getCreatedOn().compareTo(o2.getCreatedOn());
			}
		});
		return tasks;
	}

	/**
	 * This method persists a new Task instance
	 * @param task
	 * 
	 * DONE: Add implementation to also update the Cache with the new object
	 */
	public void insert(Task task) {
		if(task.getCreatedOn()==null) {
			task.setCreatedOn(new Date());
		}
		em.persist(task);
		User user = userService.getUserFromId(currentUser.getId());
		user.getTasks().add(task);
		em.merge(user);
		cache.put(currentUser.getId(),user);
	}


	/**
	 * This method persists an existing Task instance
	 * @param task
	 * 
	 * DONE: Add implementation to also update the Object in the Cache
	 */
	public void update(Task task) {
		Task newTask = em.merge(task);
		
//		cache.replace(task.getId(),newTask);
	}
	
	/**
	 * This method deletes an Task from the persistence store
	 * @param task
	 * 
	 * DONE: Add implementation to also delete the object from the Cache
	 */
	public void delete(Task task) {
		//Note object may be detached so we need to tell it to remove based on reference
		em.remove(em.getReference(task.getClass(),task.getId()));
		cache.remove(task.getId());
	}
	
	
	/**
	 * This method is called after construction of this SLSB.
	 * 
	 * DONE: Replace implementation to read existing Tasks from the database and add them to the cache
	 */
	@PostConstruct
	public void startup() {
		
		log.info("### Querying the database for tasks!!!!");
		final CriteriaQuery<Task> criteriaQuery = em.getCriteriaBuilder().createQuery(Task.class);
		Collection<Task> resultList = em.createQuery(
				criteriaQuery.select(
						criteriaQuery.from(Task.class)
						)
				).getResultList();
		
		for (Task task : resultList) {
			this.insert(task);
		}
		
	}
	
}

package org.magnum.mobilecloud.video.repository;

import java.util.Collection;

import org.springframework.data.repository.CrudRepository;

public interface DkVideoRepository extends CrudRepository<Video, Long> {
	
	public Collection<Video> findByName(String name);
	
	public Collection<Video> findByDurationLessThan(Long duration);
	
}

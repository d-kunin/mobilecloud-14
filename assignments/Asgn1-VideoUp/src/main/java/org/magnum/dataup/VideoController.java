/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {

	/**
	 * You will need to create one or more Spring controllers to fulfill the
	 * requirements of the assignment. If you use this file, please rename it
	 * to something other than "AnEmptyController"
	 * 
	 * 
		 ________  ________  ________  ________          ___       ___  ___  ________  ___  __       
		|\   ____\|\   __  \|\   __  \|\   ___ \        |\  \     |\  \|\  \|\   ____\|\  \|\  \     
		\ \  \___|\ \  \|\  \ \  \|\  \ \  \_|\ \       \ \  \    \ \  \\\  \ \  \___|\ \  \/  /|_   
		 \ \  \  __\ \  \\\  \ \  \\\  \ \  \ \\ \       \ \  \    \ \  \\\  \ \  \    \ \   ___  \  
		  \ \  \|\  \ \  \\\  \ \  \\\  \ \  \_\\ \       \ \  \____\ \  \\\  \ \  \____\ \  \\ \  \ 
		   \ \_______\ \_______\ \_______\ \_______\       \ \_______\ \_______\ \_______\ \__\\ \__\
		    \|_______|\|_______|\|_______|\|_______|        \|_______|\|_______|\|_______|\|__| \|__|
                                                                                                                                                                                                                                                                        
	 * 
	 */
	
	
	public static final String VIDEO_PATH = "/video";
	public static final String VIDEO_DATA_PATH = "/video/{id}/data";
	
	private final ConcurrentHashMap<Long, Video> mVideos = new ConcurrentHashMap<>();
	private final AtomicLong mIds = new AtomicLong();


	@RequestMapping(value=VIDEO_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return mVideos.values();
	}
	
	@RequestMapping(value=VIDEO_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v) {
		
		final long currentId = mIds.incrementAndGet();
		v.setId(currentId);
		v.setDataUrl(getDataUrl(currentId));
		
		mVideos.put(currentId, v);
		return v;
	}
	
	
	@RequestMapping(value=VIDEO_DATA_PATH, method=RequestMethod.POST)
	public @ResponseBody VideoStatus save(
						@RequestParam("data") MultipartFile file, 
						@PathVariable("id") long id) 
	{
		final Video video = mVideos.get(id);
		if (video == null) {
			send404();
		} else {
			try {
				VideoFileManager.get().saveVideoData(video, file.getInputStream());
			} catch (IOException e) {
				send500();
			}
		}
		
		return new VideoStatus(VideoState.READY);
	}
	
	private void send500() {
		throw new Http500();
	}

	private void send404() {
		throw new Http404();
	}

	@RequestMapping(value=VIDEO_DATA_PATH, method=RequestMethod.GET)
	public void getVideo(@PathVariable("id") long id,
					     HttpServletResponse response) 
	{
		final Video video = mVideos.get(id);
		if (video == null) {
			send404(id, response);
		} else {
			try {
				VideoFileManager.get().copyVideoData(video, response.getOutputStream());
				response.flushBuffer();
			} catch (IOException e) {
				send500(id, response);
			}
		}
	}

	// utility stuff
    private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

 	private String getUrlBaseForLocalServer() {
	   HttpServletRequest request = 
	       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	   String base = 
	      "http://" + request.getServerName() + ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");
	   return base;
	}
 	
	private void send404(long id, HttpServletResponse response) {
		try {
			response.sendError(HttpStatus.NOT_FOUND.value(), "Video not found: " + id);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void send500(long id, HttpServletResponse response) {
		try {
			response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to stream video: " + id);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	private static class Http404 extends RuntimeException {	
	}
	
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	private static class Http500 extends RuntimeException {	
	}
}

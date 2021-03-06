package de.adorsys.ledgers.postings.impl.converter;

import org.springframework.stereotype.Component;

import de.adorsys.ledgers.postings.api.domain.PostingLineBO;
import de.adorsys.ledgers.postings.db.domain.PostingLine;
import de.adorsys.ledgers.util.CloneUtils;

@Component
public class PostingLineMapper {
	
    public PostingLineBO toPostingLineBO(PostingLine posting) {
    	return CloneUtils.cloneObject(posting, PostingLineBO.class);
    }

    public PostingLine toPostingLine(PostingLineBO posting) {
    	return CloneUtils.cloneObject(posting, PostingLine.class);    	
    }
}

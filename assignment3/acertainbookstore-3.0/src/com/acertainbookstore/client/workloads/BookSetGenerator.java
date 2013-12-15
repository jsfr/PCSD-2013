package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {
	Random r = new Random();
	
	public BookSetGenerator() {
	}

	/**
	 * Returns num randomly selected isbns from the input set
	 * 
	 * @param num
	 * @return
	 */
	public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) {
		
		List<Integer> tmp = new ArrayList<Integer>(isbns);
		Set<Integer> ret = new HashSet<Integer>();
		
		Collections.shuffle(tmp);
		if (num < tmp.size()) {
		    tmp = tmp.subList(0, num);
		}
		ret.addAll(tmp);
		
		return ret;
	}

	/**
	 * Return num stock books. For now return an ImmutableStockBook
	 * 
	 * @param num
	 * @return
	 */
	public Set<StockBook> nextSetOfStockBooks(int num) {
		Set<StockBook> retval = new HashSet<StockBook>(num);
		Set<Integer> isbns = new HashSet<Integer>();
		for(int i = 0; i < num; i++) {
			Integer id = r.nextInt();
			
			//Ensure unique isbn each time
			Integer isbn = Math.abs(r.nextInt());
			while(isbns.contains(isbn)){
				isbn = Math.abs(r.nextInt());
			}
			isbns.add(isbn);
			
			String name = id.toString();
			String author = id.toString();
			Float price = Math.abs(r.nextFloat());
			Integer copies = r.nextInt(10) + 1;
			Long saleMisses = 0L;
			Long timesRated = 0L;
			Long totalRating = 0L;
			Boolean editorPick = r.nextBoolean();

			StockBook book = new ImmutableStockBook(
					isbn,
					name, 
					author, 
					price, 
					copies, 
					saleMisses,
					timesRated,
					totalRating,
					editorPick);
			retval.add(book);
		}
		
		return retval;
	}

}

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Thread safe version of InvertedIndex.
 * @author JRRed
 *
 */
public class ThreadSafeInvertedIndex extends InvertedIndex {
	/** lock used for synchronization */
	private final SimpleReadWriteLock lock;
	
	/**
	 * Constructor
	 */
	public ThreadSafeInvertedIndex() {
		super();
		this.lock = new SimpleReadWriteLock();
	}
	
	@Override
	public void add(String str, String location, int position) {
		lock.writeLock().lock();
		try {
			super.add(str,  location,  position);
		}
		finally {
			lock.writeLock().unlock();
		}
	}
	
	@Override
	public Set<String> get() {
		return lock.syncSupplier(super::get, false);
	}
	
	@Override
	public Set<String> get(String str) {
		return lock.syncFunction(super::get, str, false);
	}
	
	@Override
	public Set<Integer> get(String str, String location) {
		return lock.syncBiFunction(super::get, str, location, false);
	}
	
	@Override
	public Map<String, Integer> getCounts() {
		return lock.syncSupplier(super::getCounts, false);
	}
	
	@Override
	public int countsSize() {
		return lock.syncSupplier(super::countsSize, false);
	}

	@Override
	public int countsSize(String location) {
		return lock.syncFunction(super::countsSize, location, false);
	}
	
	@Override
	public boolean contains(String str) {
		return lock.syncFunction(super::contains, str, false);
	}
	
	@Override
	public boolean contains(String str, String location) {
		return lock.syncBiFunction(super::contains, str, location, false);
	}
	
	@Override
	public boolean contains(String str, String location, int position) {
		lock.readLock().lock();
		
		try {
			return contains(str, location, position);
		}
		finally {
			lock.readLock().unlock();
		}
	}
	
	@Override
	public int size() {
		return lock.syncSupplier(super::size, false);
	}

	@Override
	public int size(String str) {
		return lock.syncFunction(super::size, str, false);
	}
	
	@Override
	public int size(String str, String location) {
		return lock.syncBiFunction(super::size, str, location, false);
	}
	
	@Override
	public String toString() {
		return lock.syncSupplier(super::toString, false);
	}
	
	@Override
	public String toJson() {
		return lock.syncSupplier(super::toJson, false);
	}
	
	@Override
	public void toJson(Path path) throws IOException {
		lock.readLock().lock();
		
		try {
			super.toJson(path);
		}
		finally {
			lock.readLock().unlock();
		}
	}
	
	@Override
	public String countsToJson() {
		return lock.syncSupplier(super::countsToJson, false);
	}
	
	@Override
	public void countsToJson(Path path) throws IOException {
		lock.readLock().lock();
		try {
			super.countsToJson(path);
		}
		finally {
			lock.readLock().unlock();
		}
	}
	
	@Override
	public List<SearchResult> exactSearch(Set<String> stems) {
		return lock.syncFunction(super::exactSearch, stems, false);
	}
	
	@Override
	public List<SearchResult> partialSearch(Set<String> stems) {
		return lock.syncFunction(super::partialSearch, stems, false);
	}
	
	@Override
	public void attemptMergeWith(InvertedIndex other) {
		lock.syncConsumer(super::attemptMergeWith, other, true);
	}
}

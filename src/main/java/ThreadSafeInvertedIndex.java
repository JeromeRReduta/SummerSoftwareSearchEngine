import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;


/**
 * Thread safe version of InvertedIndex.
 * @author JRRed
 *
 */
public class ThreadSafeInvertedIndex extends InvertedIndex {
	
	/** lock used for synchronization */
	private final SimpleReadWriteLock lock = new SimpleReadWriteLock();
	
	// TODO: Override ALL public methods below w/ synchronized version
	
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
	public Set<String> getStrings() {
		return lock.synchronizeWithSupplier(super::getStrings, false);
	}
	
	/**
	 * Returns an unmodifiable view of the strings in the index, starting with a given string (or if that string isn't in the map,
	 * starting with the first string in the map after the given string)
	 * @param start string
	 * @return An unmodifiable view of the index. If the string exists in the map, this set will start with that given string. Otherwise,
	 * this set will start with the first string in the map that comes after the given string.
	 */
	@Override
	public Set<String> getStringsStartingWith(String start) {
		return lock.synchronizeWithFunction(super::getStringsStartingWith, start, false);
	}
	
	@Override
	public Set<String> getLocationsContaining(String str) {
		return lock.synchronizeWithFunction(super::getLocationsContaining, str, false);
	}
	
	@Override
	public Set<Integer> getPositionsOfStringInLocation(String str, String location) {
		return lock.synchronizeWithBiFunction(super::getPositionsOfStringInLocation, str, location, false);
	}
	
	@Override
	public Map<String, Integer> getStringCount() {
		return lock.synchronizeWithSupplier(super::getStringCount, false);
	}

	@Override
	public int numOfTimesStringAppearsInLocation(String location) {
		return lock.synchronizeWithFunction(super::numOfTimesStringAppearsInLocation, location, false);
	}
	
	@Override
	public boolean contains(String str) {
		return lock.synchronizeWithFunction(super::contains, str, false);
	}
	
	@Override
	public boolean contains(String str, String location) {
		return lock.synchronizeWithBiFunction(super::contains, str, location, false);
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
	public int numOfStrings() {
		return lock.synchronizeWithSupplier(super::numOfStrings, false);
	}

	@Override
	public int numOfLocationsContainingString(String str) {
		return lock.synchronizeWithFunction(super::numOfLocationsContainingString, str, false);
	}
	
	@Override
	public int numOfTimesStringAppearsInLocation(String str, String location) {
		return lock.synchronizeWithBiFunction(super::numOfTimesStringAppearsInLocation, str, location, false);
	}
	
	@Override
	public String toString() {
		return lock.synchronizeWithSupplier(super::toString, false);
	}
	
	@Override
	public String toJson() {
		return lock.synchronizeWithSupplier(super::toJson, false);
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
	public String stringCountsToJson() {
		return lock.synchronizeWithSupplier(super::stringCountsToJson, false);
	}
	
	@Override
	public void stringCountsToJson(Path path) throws IOException {
		lock.readLock().lock();
		try {
			super.stringCountsToJson(path);
		}
		finally {
			lock.readLock().unlock();
		}
	}
	
	@Override
	public Collection<SearchResult> exactSearch(Set<String> stems) {
		return lock.synchronizeWithFunction(super::exactSearch, stems, false);
	}
	
	@Override
	public Collection<SearchResult> partialSearch(Set<String> stems) {
		return lock.synchronizeWithFunction(super::partialSearch, stems, false);
	}
	
	@Override
	public void attemptMergeWith(InvertedIndex other) {
		if ( !(other instanceof ThreadSafeInvertedIndex) ) return;
		lock.synchronizeWithConsumer(super::attemptMergeWith, other, true);
		
	}
}

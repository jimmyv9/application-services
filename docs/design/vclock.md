


#  Vector Clocks

## Vector Clock Usage

-   Goal of a vector clock: detect the difference between stale and conflicting data
-  Vector clocks do not help perform merges, only to detect when merges are necessary
-  Despite its name, it’s not a vector, and it does not measure time.

		HashMap<ClientId, u64>, 
		//local id and per-client change counter
    
-   After client makes changes to some item, change counter is incremented, and the value in the vector clock is set to the current value of the change counter

-   Algorithm for generating a partial ordering of events in a distributed system and detecting causality violations

-   Ability to help maintain continuity:
	- Tree stores (bookmarks)
	- Log stores (history)
	- Record stores (user information)
    
-   Issues with vector clocks: tend to grow without bound
    

## Examples of Vector Clocks


### application-services/components/remerge/src/vclock.rs

	pub struct VClock = BTreeMap<crate::Guid, Counter: u64>

	pub enum ClockOrdering {
		Equivalent,
		Ancestor,
		Descendent,
		Conflicting,
	}
#### Implementation Functions   
- Create new(): constructor of a new VClock object
	
		pub fn new(own_client_id: Guid, counter: Counter) -> Self {
			VClock(std::iter::once((own_client_id, counter)).collect())
		}
    
-   get_ordering(): compares two VClocks and determines ClockOrdering state for the self VClock in comparison to the other VClock
    
	    pub fn get_ordering(&self, other: &VClock) -> ClockOrering {}
  
- get() and increment(): get and increment the clock counter for a specific user id
- There exists functions that allow for the comparison of two VClocks:
	
		pub fn is_equivalent(&self, o: &VClock) -> bool {}
		pub fn is_ancestor_of(&self, o: &VClock) -> bool {}
		pub fn is_descendent_of(&self, o: &VClock) -> bool {}
		pub fn is_conflicting(&self, o: &VClock) -> bool {}
    
-   There exists the ability to turn a VClock into an iterator type:

		impl<'a> IntoIterator for &'a VClock {}
    
-   Impl PartialOrd: allows for the running of partial ordering logic on a set of VClocks

		fn partial_cmp(&self, other: &VClock) -> Option<std::cmp::Ordering> {}
    
-   Imp ToSql: allows the information to become stored in an SQL database

		fn to_sql(&self) -> rusqlite::Result<ToSqlOutput<'_>> {}
    
-   Impl FromSql: allows for the ability to pull information from the database and access it as a FromSqlResult

		fn column_result(value: ValueRef<'_>) -> FromSqlResult<Self> {}

### application-services/components/remerge/src/storage/db.rs

	pub struct RemergeDb{
		db: Connection,
		info: SchemaBundle,
		client_id: sync_guid::Guid,
	}
     
- In function create(&self, native: &NativeRecord) -> Result<Guid> within impl RemergeDb, we find the initialization of a VClock:
    
	    let ctr = self.counter_bump()?;
	    let vclock = VClock::new(self.client_id(), ctr);
    
- To access a client's VClock from the database, there exists:

		fn get_vclock(&self, id: &str) -> Result<VClock> {
			Ok(self.db.query_row_named(
				"SELECT vector_clock FROM rec_local
				  WHERE guid = :guid AND is_deleted = 0
				  UNION ALL
				  SELECT vector_clock FROM rec_mirror
				  WHERE guid = :guid AND is_overridden IS NOT 1",
				 named_params! { ":guid": id },
				 |row| row.get(0),
			)?)
		}

- To increment the VClock counter, there is the function:

		fn counter_bump(&self) -> Result<Counter> {}

- To produce a VClock with the bumped counter, there exists:

		fn get_bumped_vclock(&self, id: &str) -> Result<VClock> {
			let vc = self.get_vclock(id)?;
			let counter = self.counter_bump()?;
			Ok(vc.apply(self.client_id.clone(), counter))
		}
- We see the function get_bumped_vclock being used within:
    
		pub fn update_record(&self, record: &NativeRecord) -> Result<()>{}
- As the database needs to be synchronized between the Local and Native Records, the updated record sets the VClock as:
	
		let vclock = self.get_bumped_vclock(&guid)?;


	

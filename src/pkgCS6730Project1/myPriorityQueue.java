package pkgCS6730Project1;

/**
 * Priority Queue class implementation to be used for FEL
 * @author john
 *
 * @param <T> comparable object this queue will hold.  comparator determines priority
 */
public abstract class myPriorityQueue<T extends Comparable<T>>{
	//initially have room for initSize items; start index of heap array - using 1 so that heap idxes line up to 2x / 2x + 1
	protected final int initSize = 10, _stIDX = 1;
	//size of heap array holding elements in queue;//# of elements in heap/queue
	protected int _size, _numElems;
	//array data structure holding data - using heap structure - ignore idx 0, using idxs 1->_size-1
	protected T[] heap;
	
	public myPriorityQueue() {
		initHeap();	
	}//empty priority queue
	
	//ctor for initial size to be different than initSize
	public myPriorityQueue(int _initSize) {resizeHeap(_initSize);	}
	//ctor for initial set of keys
	public myPriorityQueue( T[] _keys) {		
		resizeHeap(_keys.length * 2);//make 2x as large as initial key set
		System.arraycopy(_keys, _stIDX, heap, _stIDX, _keys.length);
		_numElems = _keys.length;		
	}

	//call to reinitialize/clear heap
	public void initHeap() {
		_numElems = 0;
		resizeHeap(initSize);	
	}//
	
	//remake heap array for new size - only do when current size is too small - need to then heapify
	//cannot shrink heap smaller than current num elems.._newSize needs to be bigger than _numElems
	//ignores elems idx < _stIDX
	private void resizeHeap(int _newSize) {
		if(_newSize < _numElems) {return;}//no change if trying to shrink smaller than # of elements in queue
		T[] tmpHeap = buildCompAra(_newSize+_stIDX);//ignore idx 0
		//if heap already exists, copy current heap into tmpHeap
		if((null != heap) && (_numElems > 0) && (_numElems < heap.length)){//copy from heap into tmpHeap	- heap should already be in appropriate order, so no need to reheapify
			System.arraycopy(heap, _stIDX, tmpHeap, _stIDX, _numElems);	
		}
		heap = tmpHeap;
		_size = _newSize;		
	}//resizeHeap
		
	public void insert(T elem) {
		if(isFull()) {resizeHeap(_size*2);}//increase size of heap before adding if heap is full
        // double size of array if necessary
        heap[++_numElems] = elem;
        up(heap,_numElems);		//heapify toward leaves
        if (!isHeap(heap)) {System.out.println("HEAP ERROR : Heapness not preserved after attempting to add element : "+ elem);}
	}//insert

	//peek at 1st element from list
	public T peekFirst() {
		if(isEmpty()) {return null;}
		return heap[_stIDX];
	}
	public T peekElem(int idx) {
		if(isEmpty() || idx > _numElems) {return null;}
		return heap[idx];
	}
	
	//remove and return idx'th element from heap
	protected T removeElem(int idx) {
		if(isEmpty()) {return null;}
		T elem = heap[idx];
		swap(heap, idx, _numElems);		//move element to remove from heap to end of heap ara
        heap[_numElems] = null;     	//remove old elem	
		--_numElems;					//shrink # of elements
        down(heap, idx, _numElems);					
 
        //shrink heap to half size, if possible
        if ((_numElems > 0) && (_numElems <= _size / 3)) {resizeHeap(_size / 2);}
        if (!isHeap(heap)) {System.out.println("HEAP ERROR : Heapness not preserved after attempting to remove element  " + elem + " at idx : "+ idx);}
		return elem;		
	}//removeMin
	
	//remove and return first element - _stIDX is location of element
	public T removeFirst() {return removeElem(_stIDX);}	
	//remove and return passed element - not necessarily the first element
	public T removeElem(T elem) {
		if(isEmpty()) {return null;}
		int idx = findElem(elem);
		if(-1==idx) {return null;}				//not found
		return removeElem(idx); 
	}
	
	//find index of element elem
	private int findElem(T elem) {
		for(int i=_stIDX; i<=_numElems;++i) {			if(heap[i].compareTo(elem) == 0) {return i;}		}
		return -1;
	}
	
    //////////////////////////
	//tests for heapness
	public boolean isHeap() {return isHeap(heap,_stIDX);}
    protected boolean isHeap(T[] ara) {return isHeap(ara,_stIDX);}
    protected boolean isHeap(T[] ara, int idx) {
        if (idx > _numElems) return true;
        int left = 2*idx;
        if (left  <= _numElems && compare(ara,idx, left))  return false;
        int right = left + 1;
        if (right <= _numElems && compare(ara,idx, right)) return false;
        return isHeap(ara,left) && isHeap(ara,right);
    }//verify this is an appropriate heap (either min or max)
    
    //move elements based on comparison
    //heapify up tree
    protected void up(T[] ara, int idx) {
    	while (idx > 1 && compare(ara,idx/2, idx)) {		swap(ara, idx, idx/2);	idx = idx/2; } 
    }//up
    //heapify down tree ara that has n elments, starting at idx 
    protected void down(T[] ara,int idx, int n) {
    	int j;
        while (2*idx <= n) {
            j = 2*idx;
            if (j < n && compare(ara,j, j+1)){++j;}
            if (!compare(ara,idx, j)) break;
            swap(ara, idx, j);
            idx = j;
        }
    }//down
    
    //build array of comparables of passed size
    private T[] buildCompAra(int size) {
    	@SuppressWarnings("unchecked")
    	T[] res = (T[]) new Comparable[size];   
    	return res;
    }
    
    //return all members of heap in sorted order
    public T[] getSortedElems() {
		T[] res = buildCompAra(_numElems+_stIDX);
    	System.arraycopy(heap, _stIDX, res, _stIDX, _numElems);
    	int n = _numElems;
    	for (int i=n/2; i>0; --i) {  		down(res, i, n); 	} 
    	while (n>1) {   		swap(res, 1, n--);		down(res, 1, n);   	}
    	
		T[] res2 = buildCompAra(_numElems);
    	System.arraycopy(res, _stIDX, res2, 0, _numElems);    	
        if (!isHeap(heap)) {System.out.println("HEAP ERROR : Heapness not preserved after attempting to build sorted ara of elements");}   	
    	return res2;    	
    }//getSortedElems
    
    //either min or max heap
	protected abstract boolean compare(T[] ara, int i, int j);
    protected void swap(T[] ara, int i, int j) { T s = ara[i];ara[i] = ara[j];ara[j] = s;}
    
	public boolean isEmpty() {return (_numElems == 0);}
	public boolean isFull() {return (_numElems == _size);}
	public int size() {return _numElems;}	
	//for debugging only
	public T[] getHeap() {return heap;}
	
}//class myPriorityQueue

class myMinQueue<T extends Comparable<T>> extends myPriorityQueue<T>{	
	public myMinQueue() {super();}//empty min priority queue of default size
	public myMinQueue(int _initSize) {super(_initSize);	}
	public myMinQueue( T[] _keys) {	super(_keys);}	
	@Override
    protected boolean compare(T[] ara, int i, int j) {	return ara[i].compareTo(ara[j]) > 0;}	    
	
}//myMinQueue

class myMaxQueue<T extends Comparable<T>> extends myPriorityQueue<T>{
	public myMaxQueue() {super();}//empty max priority queue of default size
	public myMaxQueue(int _initSize) {super(_initSize);	}
	public myMaxQueue( T[] _keys) {	super(_keys);}	
	@Override
	protected boolean compare(T[] ara, int i, int j) {	return ara[i].compareTo(ara[j]) < 0;}	
	
}//myMaxQueue


# File-system
<p>This projects includes an API for reading a Linux ext2 file system. (ext2fs is a sample file representing the system and
    from which this API demos read</p>


<h3>Creating a Volume</h3>

<p>A Volume is loaded by instantiating the Volume class
    <code>Volume vol = new Volume(pathToVolume)</code>. In this case it will be the path to the ext2fs file.</p>

<h3>Creating a new file</h3>

<p>Creating a file instance is done by calling on a the Ext2File constructor and passing the volume where the file is located
    and an absolute path to it.
    <pre>
        <code>
            Volume vol = new Volume("ext2fs");
            Ext2File dirs = new Ext2File(vol, "/files/dir-e");
        </code>
    </pre>
</p>

<h3>Obtaining file metadata</h3>

<p>From here we could obtain file information like its: absolute path, size, name, parent name. And if the file is a directory
    we could find out what its sub files by calling
    <code>Ext2File.ls()</code>.</p>
<img src="https://github.com/Paketche/File-system/blob/master/pics/file%20info.PNG" style="display: block;">


<h3>Reading from a file</h3>

<p>Reading from a file could be done in several ways.
    <ul>
        <li>By using the
            <code>Ext2File.readFully()</code> method which returns an array of the file's bytes</li>
        <li>By reading the file data in chunks
            <div>
                <img src="https://github.com/Paketche/File-system/blob/master/pics/reading%20in%20chunks.PNG" alt="">
            </div>
        </li>
        <li>By reading the last bytes from a file
            <div>
                <img src="https://github.com/Paketche/File-system/blob/master/pics/reading%20last%2010%20byte%20from%20a%20file.PNG" alt="">
            </div>
        </li>
    </ul>
</p>

<h3>Traversing the file tree</h3>

<p>This could be done by passing an Ext2File to
    <code>Volume.traverse(file, indent)</code> method. This will traverse the subdirectories recursively, stopping at any non-directory
    files and printing out the file names. The indentation variable from which the tree will start. Here are photos of the operation. The full contents have been omitted since it's idea is to test the API could handle big directories
    <div>
        <img src="https://github.com/Paketche/File-system/blob/master/pics/recursive%20travelsal%20part%201.PNG" alt="">
    </div>
    <div>
        <img src="https://github.com/Paketche/File-system/blob/master/pics/recursive%20travelsal%20part%202.PNG" alt="">
    </div>
</p>

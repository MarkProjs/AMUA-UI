import React, { useEffect, useState } from 'react';
import {
    fetchCompanies,
    fetchClasses,
    fetchBusinessUnit,
    searchWithParams,
    searchAll,
    searchByCompany,
    fetchAllBUs,
    addClassForCompany,
    getNTAccountsByCompany
} from '../services/api';
import DropDownSelector from '../components/DropdownSelector';
import SearchTable from '../components/SearchTable';
import './styles/Home.css';
import {motion, AnimatePresence} from 'framer-motion';

function Home() {
    const [hasSearched, setHasSearched] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [companies, setCompanies] = useState([]);
    const [selectedCompany, setSelectedCompany] = useState('');
    const [classes, setClasses] = useState([]);
    const [selectedClass, setSelectedClass] = useState('');
    const [businessUnits, setBusinessUnits] = useState([]);
    const [selectedBU, setSelectedBU] = useState('');
    const [results, setResults] = useState([]);
    const [showAddSection, setShowAddSection] = useState(false);
    const [newClass, setNewClass] = useState('');
    const [newCompany, setNewCompany] = useState('');
    const [ntAccounts, setNtAccounts] = useState([]);
    const [selectedNTAccount, setSelectedNTAccount] = useState('');


    useEffect(() => {
        async function loadCompanies() {
            const data = await fetchCompanies();
            setCompanies(data.map(item => item.Company));
        }

        loadCompanies();
    }, []);


    useEffect(() => {
        async function loadClasses() {
            if (selectedCompany) {
                const data = await fetchClasses(selectedCompany);
                setClasses(data.map(item => item.Class));
            } else {
                setClasses([]);
            }
        }
        loadClasses();
    }, [selectedCompany]);

    useEffect(() => {
        async function loadNTAccounts() {
            if (newCompany) {
                const accounts = await getNTAccountsByCompany(newCompany);
                setNtAccounts(accounts);
            } else {
                setNtAccounts([]);
            }
        }
        loadNTAccounts();
}, [newCompany]);



    async function handleSearch() {
        try {
            setHasSearched(true);
            if (selectedCompany && selectedClass) {
                const buData = await fetchBusinessUnit(selectedCompany, selectedClass);
                if (!buData) return setResults([]);
                const data = await searchWithParams(selectedCompany, selectedClass, buData);
                setResults(data);
            } else if (selectedCompany) {
                const data = await searchByCompany(selectedCompany);
                setResults(data);
            } else {
                const data = await searchAll();
                setResults(data);
            }
        } catch (e) {
            console.error('Search failed', e);
        }
    }

async function handleAddClass() {
    try {
        setIsSaving(true);
        const result = await addClassForCompany(newCompany, newClass, selectedBU, selectedNTAccount);

        if (!result.success) {
            alert(result.message);  
            return;
        }

        const classData = await fetchClasses(newCompany);
        setClasses(classData.map(item => item.Class));
        setSelectedClass(newClass);
        setSelectedCompany(newCompany);
        setShowAddSection(false);
        setSelectedBU('');
        setNewClass('');
        setNewCompany('');
        setSelectedNTAccount('');
        await handleSearch();
    } catch (e) {
        console.error('Failed to add class', e);
        alert('Failed to add class. Please try again.');
    } finally {
        setIsSaving(false);
    }
}



    return (
        <div className="page-wrapper">
            <div className="header-bar">Director Role Maintenance</div>
            <div className="filter-bar">
                <DropDownSelector
                    label="Company Code"
                    value={selectedCompany}
                    options={companies}
                    onChange={(e) => setSelectedCompany(e.target.value)}
                    placeholder="Select company"
                />
                <DropDownSelector
                    label="Class Codes"
                    value={selectedClass}
                    options={classes}
                    onChange={(e) => setSelectedClass(e.target.value)}
                    placeholder="Select class"
                    isSearchable={true}
                />
                <div className="d-flex align-items-end ms-3">
                    <button className="btn btn-dark" onClick={handleSearch}>Search</button>
                </div>
            </div>
            <div className="action-bar">
                <button
                    className="btn btn-success me-2"
                    onClick={async ()=>{
                        const newState = !showAddSection;
                        setShowAddSection(newState);

                        if (newState) {
                            setResults([]); 
                            setHasSearched(false); 
                        }
                        
                        if (newState && businessUnits.length === 0) {
                            try {
                                const data = await fetchAllBUs();
                                const uniqueBU = [...new Set(data.map(item => item.BusinessUnit))];
                                setBusinessUnits(uniqueBU);
                            } catch (e) {
                                console.error('Failed to fetch all BUs', e);
                            }
                        }
                    }}
                >
                    {showAddSection ? '- Cancel': '+ Add new record'}
                </button>
            </div>
            <AnimatePresence>
                {showAddSection && (
                    <motion.div
                        key="add-form"
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        transition={{ duration: 0.3 }}
                        className="add-form card mb-3 p-3"
                    >
                        <div className="d-flex flex-wrap align-items-end">
                            <div className="form-group me-4">
                                <DropDownSelector
                                    label="Company"
                                    value={newCompany}
                                    options={companies}
                                    onChange={(e) => setNewCompany(e.target.value)}
                                    placeholder="Select company"
                                />
                            </div>

                            <div className="form-group me-4">
                                <DropDownSelector
                                    label="Business Unit"
                                    value={selectedBU}
                                    options={businessUnits}
                                    onChange={(e) => setSelectedBU(e.target.value)}
                                    placeholder="Select BU"
                                    isSearchable={true}
                                />
                            </div>

                            <div className="form-group me-4">
                                <label htmlFor="newClass" className="form-label">New Class</label>
                                <input
                                    id="newClass"
                                    type="text"
                                    className="form-control"
                                    value={newClass}
                                    onChange={(e) => setNewClass(e.target.value)}
                                    placeholder="Enter new class code"
                                />
                            </div>

                            <div className="form-group me-4">
                                <DropDownSelector
                                    label="Director NT Account"
                                    value={selectedNTAccount}
                                    options={ntAccounts}
                                    onChange={(e) => setSelectedNTAccount(e.target.value)}
                                    placeholder="Select NT Account"
                                    isSearchable={true}
                                />
                            </div>

                            <div className="form-group">
                                <button
                                    className="btn btn-primary"
                                    onClick={handleAddClass}
                                    disabled={!newCompany || !selectedBU || !newClass || !selectedNTAccount || isSaving}
                                >
                                    {isSaving ? 'Saving...' : 'Save'}
                                </button>
                            </div>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
            <SearchTable 
                        data={results} 
                        onUpdateComplete={handleSearch} 
                        addingMode={showAddSection}
                        hasSearched={hasSearched}
            />
        </div>
    );
}

export default Home;

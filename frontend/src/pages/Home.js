import React, { useEffect, useState } from 'react';
import {
    fetchCompanies,
    fetchClasses,
    fetchAllClasses,
    fetchBusinessUnit,
    fetchBusinessUnitForClass,
    searchWithParams,
    searchAll,
    searchByCompany,
    addDirector,
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
    const [results, setResults] = useState([]);
    const [showAddSection, setShowAddSection] = useState(false);
    
    // For the add new record form
    const [addFormCompany, setAddFormCompany] = useState('');
    const [addFormClasses, setAddFormClasses] = useState([]);
    const [addFormSelectedClass, setAddFormSelectedClass] = useState('');
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

    // Load classes for the add form when company is selected (get ALL classes from BUxClass)
    useEffect(() => {
        async function loadAddFormClasses() {
            if (addFormCompany) {
                const data = await fetchAllClasses(addFormCompany);
                setAddFormClasses(data.map(item => item.Class));
            } else {
                setAddFormClasses([]);
            }
        }
        loadAddFormClasses();
    }, [addFormCompany]);

    useEffect(() => {
        async function loadNTAccounts() {
            if (addFormCompany) {
                const accounts = await getNTAccountsByCompany(addFormCompany);
                setNtAccounts(accounts.map( a => ({ value: a.value, label: a.label })));
            } else {
                setNtAccounts([]);
            }
        }
        loadNTAccounts();
    }, [addFormCompany]);

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


async function handleAddDirector() {
    if (!addFormCompany || !addFormSelectedClass || !selectedNTAccount) {
        alert('Please fill in all required fields');
        return;
    }

    try {
        setIsSaving(true);

        // Get the business unit for the selected class
        const businessUnit = await fetchBusinessUnitForClass(addFormCompany, addFormSelectedClass);
        if (!businessUnit) {
            alert('Could not find business unit for the selected class. Please ensure the class exists in BUxClass table.');
            return;
        }

        const result = await addDirector(addFormCompany, addFormSelectedClass, businessUnit, selectedNTAccount);

        if (!result.success) {
            alert(result.message);
            return;
        }

        // Reset the add form fields
        setAddFormCompany('');
        setAddFormSelectedClass('');
        setSelectedNTAccount('');
        setShowAddSection(false);

        // ✅ Refresh the Class Code dropdown in the Search section
        if (addFormCompany) {
            const updatedClasses = await fetchClasses(addFormCompany);
            setClasses(updatedClasses.map(item => item.Class));
        }

        if (hasSearched) {
            await handleSearch();
        }

        alert('Director assigned successfully!');
    } catch (e) {
        console.error('Failed to assign director', e);
        alert('Failed to assign director. Please try again.');
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
                    onClick={() => {
                        const newState = !showAddSection;
                        setShowAddSection(newState);

                        if (newState) {
                            setResults([]); 
                            setHasSearched(false); 
                            // Reset add form when opening
                            setAddFormCompany('');
                            setAddFormSelectedClass('');
                            setSelectedNTAccount('');
                        }
                    }}
                >
                    {showAddSection ? '- Cancel': '+ Assign a Director'}
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
                        <h5 className="mb-3">Assign New Director</h5>
                        <div className="d-flex flex-wrap align-items-end">
                            <div className="form-group me-4">
                                <DropDownSelector
                                    label="Company"
                                    value={addFormCompany}
                                    options={companies}
                                    onChange={(e) => setAddFormCompany(e.target.value)}
                                    placeholder="Select company"
                                />
                            </div>

                            <div className="form-group me-4">
                                <DropDownSelector
                                    label="Existing Class"
                                    value={addFormSelectedClass}
                                    options={addFormClasses}
                                    onChange={(e) => setAddFormSelectedClass(e.target.value)}
                                    placeholder="Select existing class"
                                    isSearchable={true}
                                />
                            </div>

                            <div className="form-group me-4">
                                <DropDownSelector
                                    label="New Director Name"
                                    value={selectedNTAccount}
                                    options={ntAccounts}
                                    onChange={(e) => setSelectedNTAccount(e.target.value)}
                                    placeholder="Select Name..."
                                    isSearchable={true}
                                />
                            </div>

                            <div className="form-group">
                                <button
                                    className="btn btn-primary"
                                    onClick={handleAddDirector}
                                    disabled={!addFormCompany || !addFormSelectedClass || !selectedNTAccount || isSaving}
                                >
                                    {isSaving ? 'Assigning...' : 'Assign Director'}
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
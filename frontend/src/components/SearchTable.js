import React, { useState, useEffect } from 'react';
import Select from 'react-select';
import {
    updateNTAccount,
    getNTAccountsByCompany,
    deleteClassFromBu,
    deleteRowFromTable
} from '../services/api';
import './styles/SearchTable.css';

function SearchTable({ data, onUpdateComplete, addingMode,hasSearched }) {
    const [editRowIndex, setEditRowIndex] = useState(null);
    const [ntValue, setNtValue] = useState('');
    const [ntDropdownOptions, setNtDropdownOptions] = useState([]);

    useEffect(() => {
        if (editRowIndex !== null) {
            const company = data[editRowIndex].Company;
            fetchNTOptions(company);
        }
    }, [data, editRowIndex]);

    async function fetchNTOptions(company) {
        try {
            const list = await getNTAccountsByCompany(company);
            const options = list.map(acc => ({ value: acc, label: acc }));
            setNtDropdownOptions(options);
        } catch (error) {
            console.error('Failed to fetch NT Account options:', error);
        }
    }

    function handleUpdateClick(index)  {
        const currentNT = data[index]?.NT_Account || ''; // Avoid null
        setEditRowIndex(index);
        setNtValue(currentNT);
    }


    async function handleSaveClick(row) {
        try {
            await updateNTAccount(row.Company, row.Class, row.BusinessUnit, ntValue);
            setEditRowIndex(null);
            if (onUpdateComplete) {
                onUpdateComplete();
            }
        } catch (error) {
            console.error('Failed to update NT_Account:', error);
        }
    }

    async function handleDeleteClick(row) {
        const confirm = window.confirm(
            `Are you sure you want to delete Class "${row.Class}" from company "${row.Company}"?`
        );
        if (!confirm) return;

        try {
            await deleteClassFromBu(row.Class, row.Company);
            await deleteRowFromTable(row.Class, row.Company);
            if (onUpdateComplete) {
                onUpdateComplete();
            }
        } catch (error) {
            console.error('Failed to delete class', error);
            alert('Failed to delete class. Please try again.');
        }
    }

    if (!data.length && !addingMode && hasSearched) {
        return <p>No data found.</p>;
    }

    if (!data.length && (!hasSearched || addingMode)) {
        return null; // Don’t render anything
    }


    return (
        <div className="table-wrapper">
            <table className="table custom-table">
                <thead>
                    <tr>
                        <th>Company</th>
                        <th>Business Unit</th>
                        <th>Class</th>
                        <th>Role</th>
                        <th>NT Account</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {data.map((row, idx) => (
                        <tr key={idx}>
                            <td>{row.Company}</td>
                            <td>{row.BusinessUnit}</td>
                            <td>{row.Class}</td>
                            <td>{row.Role}</td>
                            <td style={{ minWidth: '200px' }}>
                                {editRowIndex === idx ? (
                                    <Select
                                        value={
                                            ntDropdownOptions.find(opt => opt.value === ntValue) || null
                                        }
                                        onChange={(selectedOption) =>
                                            setNtValue(selectedOption?.value || '')
                                        }
                                        options={ntDropdownOptions}
                                        isClearable
                                        isSearchable
                                        placeholder="Select NT Account"
                                    />

                                ) : (
                                    row.NT_Account
                                )}
                            </td>
                            <td>
                                {editRowIndex === idx ? (
                                    <>
                                        <button
                                            className="btn btn-sm btn-success me-2"
                                            onClick={() => handleSaveClick(row)}
                                        >
                                            Save
                                        </button>
                                        <button
                                            className="btn btn-sm btn-secondary"
                                            onClick={() => setEditRowIndex(null)}
                                        >
                                            Cancel
                                        </button>
                                    </>
                                ) : (
                                    <>
                                        <button
                                            className="btn btn-sm btn-primary me-2"
                                            onClick={() => handleUpdateClick(idx)}
                                        >
                                            Update
                                        </button>
                                        <button
                                            className="btn btn-sm btn-danger"
                                            onClick={() => handleDeleteClick(row)}
                                        >
                                            Delete
                                        </button>
                                    </>
                                )}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export default SearchTable;

import React from 'react';
import Select from 'react-select';

function DropDownSelector({ label, value, options, onChange, placeholder, isSearchable = false }) {
    const formattedOptions = options.map(opt=>
        typeof opt === 'string' ? {value: opt, label: opt} : opt
    );
    return(
        <div className='dropdown-selector' style={{minWidth: '220px'}}>
            <label className='form-label fw-bold'>{label}</label>
            <Select
                options = {formattedOptions}
                value={formattedOptions.find(opt => opt.value === value)}
                onChange={(selected) => onChange({ target: { value: selected?.value || '' } })}
                placeholder={placeholder}
                isSearchable={isSearchable}
                classNamePrefix="react-select"
            />
        </div>
    );
}

export default DropDownSelector;